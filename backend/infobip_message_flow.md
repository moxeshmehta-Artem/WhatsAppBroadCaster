# Infobip Message Flow: Code-by-Code Explanation

This document details the exact sequence of code execution when a client makes a request to send a WhatsApp message and explicitly specifies `"provider": "infobip"`. It covers the journey from the initial POST request, through the internal service logic, to the Infobip API plugin, and finally how the system handles the asynchronous delivery response via webhooks.

---

## 1. The Entry Point: BroadcastController

It all starts when a client sends an HTTP POST request to `/api/broadcast`. The JSON body contains the `mobileNumber`, `templateId`, and importantly, the `"provider": "infobip"`.

```java
// File: src/main/java/com/example/messagebroadcast/controller/BroadcastController.java
@PostMapping
public ResponseEntity<?> broadcastMessage(@RequestBody BroadcastRequestDTO requestDTO) {
    try {
        // requestDTO.getProvider() == "infobip"
        WhatsAppLog logResult = broadcastService.processAndBroadcast(requestDTO);
        return ResponseEntity.ok(logResult);
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    } catch (Exception e) {
        return ResponseEntity.internalServerError().body(e.getMessage());
    }
}
```

---

## 2. Core Logic & Provider Routing: BroadcastService

The controller passes the `requestDTO` to the [BroadcastService](file:///home/artem/Desktop/Backend/Message_Broadcast/src/main/java/com/example/messagebroadcast/service/BroadcastService.java#21-94). The core logic takes over to assemble the final message and correctly route it to the Infobip plugin.

```java
// File: src/main/java/com/example/messagebroadcast/service/BroadcastService.java
public WhatsAppLog processAndBroadcast(BroadcastRequestDTO requestDTO) {
    log.info("Processing broadcast request for mobile {}, using provider {}", requestDTO.getMobileNumber(), requestDTO.getProvider());

    // 1. Fetch Template from DB
    WhatsAppTemp template = templateRepository.findById(requestDTO.getTemplateId())
            .orElseThrow(() -> new IllegalArgumentException("Template not found"));

    // 2. Prepare Message Content (Inject dynamic variables into template)
    String finalMessageContent = buildMessageFromTemplate(template.getContent(), requestDTO.getVariables());

    // 3. Select Provider dynamically using the Strategy Pattern
    // It loops through all loaded plugins and finds the one where getProviderName() returns "infobip"
    BroadcastProviderPlugin selectedProvider = providers.stream()
            .filter(p -> p.getProviderName().equalsIgnoreCase(requestDTO.getProvider()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unsupported provider: " + requestDTO.getProvider()));

    // Verify Infobip is active in the database
    WhatsAppProvider dbProvider = providerRepository.findByProviderNameIgnoreCaseAndStatus(selectedProvider.getProviderName(), ProviderStatus.ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("Provider not found or is INACTIVE: " + selectedProvider.getProviderName()));

    // 4. Execute the Plugin logic (Goes straight to InfobipProviderPlugin)
    SendMessageResponseDTO response = selectedProvider.sendMessage(requestDTO.getMobileNumber(), finalMessageContent);

    // 5. Save the primary execution log in DB
    WhatsAppLog messageLog = WhatsAppLog.builder()
            .mobileNo(requestDTO.getMobileNumber())
            .template(template)
            .provider(dbProvider)
            .status(response.getStatus()) // e.g. "SENT_VIA_INFOBIP"
            .externalMessageId(response.getMessageId()) // Infobip's unique ID
            .build();
    WhatsAppLog savedLog = logRepository.save(messageLog);

    // 6. Save exact timestamped detail in DB
    WhatsAppLogDetail detail = WhatsAppLogDetail.builder()
            .whatsAppLog(savedLog)
            .status(messageLog.getStatus())
            .errorMessage(response.isSuccess() ? "Success" : response.getErrorDetails())
            .build();
    logDetailRepository.save(detail);

    return savedLog;
}
```

---

## 3. Provider Execution: InfobipProviderPlugin

When `selectedProvider.sendMessage(...)` is called, the execution jumps into the [InfobipProviderPlugin](file:///home/artem/Desktop/Backend/Message_Broadcast/src/main/java/com/example/messagebroadcast/service/InfobipProviderPlugin.java#18-92). This isolated class knows exactly how to talk to Infobip's unique API structure.

```java
// File: src/main/java/com/example/messagebroadcast/service/InfobipProviderPlugin.java
@Override
public SendMessageResponseDTO sendMessage(String mobileNumber, String messageContent) {
    log.info("Sending message via Infobip to {}: {}", mobileNumber, messageContent);
    RestTemplate restTemplate = new RestTemplate();
    
    // 1. Construct Headers (Specific to Infobip)
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "App " + apiKey); 

    // 2. Format Mobile Number (Infobip hates the '+' symbol)
    String cleanMobileNumber = mobileNumber.replaceAll("[^0-9]", "");
    if (cleanMobileNumber.length() == 10) {
        cleanMobileNumber = "91" + cleanMobileNumber;
    }
    
    // 3. Build JSON Payload Structure (Specific to Infobip WhatsApp API)
    Map<String, Object> payload = new HashMap<>();
    payload.put("from", "447860088970"); // Sandbox/Sender number
    payload.put("to", cleanMobileNumber);
    Map<String, Object> content = new HashMap<>();
    content.put("text", messageContent);
    payload.put("content", content);

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

    try {
        // Enforce exact URL suffix for WhatsApp text endpoint
        String url = baseUrl.endsWith("/") ? baseUrl + "whatsapp/1/message/text" : baseUrl + "/whatsapp/1/message/text";
        
        // 4. Send the POST request to Infobip Servers
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        
        // 5. Parse the returned JSON to extract Infobip's 'messageId'
        String messageId = null;
        JsonNode root = objectMapper.readTree(response.getBody());
        if (root.has("messages") && root.get("messages").isArray() && root.get("messages").size() > 0) {
            messageId = root.get("messages").get(0).get("messageId").asText();
        }

        // Return a unified success DTO back to BroadcastService
        return new SendMessageResponseDTO(true, "SENT_VIA_INFOBIP", messageId, "Response: " + response.getBody());
        
    } catch (Exception e) {
        return new SendMessageResponseDTO(false, "FAILED_INFOBIP", null, e.getMessage());
    }
}
```

---

## 4. Catching Asynchronous Updates: WebhookController

At this point, Infobip has accepted the message to be processed. Over the next few minutes (or seconds), Infobip will attempt telecom delivery and will update your application on whether the delivery succeeded (DELIVERED, READ) or failed. Infobip achieves this by pinging your Webhook URL.

```java
// File: src/main/java/com/example/messagebroadcast/controller/WebhookController.java
@PostMapping("/infobip")
public ResponseEntity<Void> handleInfobipWebhook(@RequestBody String payload) {
    log.info("Received Infobip Webhook: {}", payload);
    try {
        // 1. Parse Infobip Webhook JSON block
        JsonNode root = objectMapper.readTree(payload);
        if (root.has("results") && root.get("results").isArray()) {
            for (JsonNode result : root.get("results")) {
                // 2. Extract crucial tracking updates
                String messageId = result.get("messageId").asText(); // Matches saved externalMessageId
                String status = result.get("status").get("name").asText(); // e.g., "DELIVERED_TO_HANDSET"
                String error = result.has("error") ? result.get("error").toString() : null;
                
                // 3. Send to service for DB updating
                webhookService.updateStatus(messageId, status, error);
            }
        }
    } catch (Exception e) {
        log.error("Error processing Infobip webhook", e);
    }
    return ResponseEntity.ok().build(); // Always respond HTTP 200 to Infobip
}
```

---

## 5. Recording the Update in DB: WebhookService

The [WebhookService](file:///home/artem/Desktop/Backend/Message_Broadcast/src/main/java/com/example/messagebroadcast/service/WebhookService.java#14-45) takes the parsed update and ensures your database perfectly reflects what Infobip reported.

```java
// File: src/main/java/com/example/messagebroadcast/service/WebhookService.java
@Transactional
public void updateStatus(String externalMessageId, String newStatus, String errorMessage) {
    log.info("Updating status for message ID: {} to {}", externalMessageId, newStatus);

    // 1. Find the exact historic initial send log using Infobip's ID
    Optional<WhatsAppLog> logEntryOpt = logRepository.findByExternalMessageId(externalMessageId);

    if (logEntryOpt.isPresent()) {
        WhatsAppLog logEntry = logEntryOpt.get();
        
        // 2. Overwrite the main status to the newest status
        logEntry.setStatus(newStatus);
        logRepository.save(logEntry);

        // 3. Create a NEW WhatsAppLogDetail record so you have a timeline
        // Ex: Database has rows: [Attempt -> SENT_VIA_INFOBIP], [Attempt -> DELIVERED_TO_HANDSET]
        WhatsAppLogDetail detail = WhatsAppLogDetail.builder()
                .whatsAppLog(logEntry)
                .status(newStatus)
                .errorMessage(errorMessage != null ? errorMessage : "Status updated via Webhook")
                .build();
        logDetailRepository.save(detail);
    } else {
        log.warn("No WhatsApp log found for external message ID: {}", externalMessageId);
    }
}
```
