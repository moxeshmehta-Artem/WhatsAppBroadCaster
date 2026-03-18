# Message Broadcast Project: In-Depth Code Flow & Architecture Analysis

This document explains the comprehensive data and execution flow of the **Message_Broadcast** Spring Boot application. It walks through step-by-step what happens internally, code-by-code, from initialization to sending a WhatsApp message and catching the delivery status.

## 1. Project Initialization & Automated Data Seeding

At the very beginning, when you start the Spring Boot application, it needs to ensure that it has the absolute minimum baseline data (like supported Providers and a dummy Template) to work immediately.

### [DatabaseSeeder.java](file:///home/artem/Desktop/Backend/Message_Broadcast/src/main/java/com/example/messagebroadcast/config/DatabaseSeeder.java) (Configuration)
```java
@Configuration
public class DatabaseSeeder {
    @Bean
    CommandLineRunner initDatabase(WhatsAppTemplateRepository templateRepo, WhatsAppProviderRepository providerRepo) {
       // Code inside runs at exact app startup...
```
**Why do we use this?**
Instead of manually typing SQL commands to add "INFOBIP" into the database every time you create a fresh database (or install the software somewhere new), the `CommandLineRunner` acts as a boot-time hook. 
- It checks if the `tbWhatsAppProvider` table is empty (`providerRepo.count() == 0`). If so, it injects **INFOBIP** and **360DIALOG** with an `ACTIVE` status.
- It also injects a default text template (**MedicalCamp**) into `tbWhatsAppTemp`.

---

## 2. Triggering a Broadcast: The Request Phase

To send a message, the client (or frontend UI) makes an HTTP POST request to `/api/broadcast`.

### [BroadcastController.java](file:///home/artem/Desktop/Backend/Message_Broadcast/src/main/java/com/example/messagebroadcast/controller/BroadcastController.java)
```java
@PostMapping
public ResponseEntity<?> broadcastMessage(@RequestBody BroadcastRequestDTO requestDTO) {
    WhatsAppLog logResult = broadcastService.processAndBroadcast(requestDTO);
    return ResponseEntity.ok(logResult);
}
```
**Why do we use this?**
Controllers act as the "gatekeepers" of the backend. They don't do the heavy lifting; they just receive raw HTTP JSON, map it into a generic Java Object ([BroadcastRequestDTO](file:///home/artem/Desktop/Backend/Message_Broadcast/src/main/java/com/example/messagebroadcast/dto/BroadcastRequestDTO.java#9-18)), and pass it to the "Manager" (the Service). 

### [BroadcastRequestDTO.java](file:///home/artem/Desktop/Backend/Message_Broadcast/src/main/java/com/example/messagebroadcast/dto/BroadcastRequestDTO.java)
```java
public class BroadcastRequestDTO {
    private String mobileNumber;
    private Long templateId;
    private String provider; // "360dialog" or "infobip"
    private Map<String, String> variables; // Mapping for {{varName}}
}
```
**Why use a DTO?**
Data Transfer Objects (DTOs) separate the incoming JSON structure from your Database Entities. You should never expose your database schema directly to external users. The DTO strictly limits the user to providing exactly what is needed—nothing more, nothing less.

---

## 3. Core Processing: The Engine Room ([BroadcastService.java](file:///home/artem/Desktop/Backend/Message_Broadcast/src/main/java/com/example/messagebroadcast/service/BroadcastService.java))

Here lies the heart of the project. The `BroadcastService::processAndBroadcast` function coordinates all database lookups and provider logic. Let's break down its internal flow.

### Step 3.1: Fetching the Template
```java
WhatsAppTemp template = templateRepository.findById(requestDTO.getTemplateId());
```
**Why?** We fetch the raw content from the database. We store generic templates like *"Hello {{name}}"* because it achieves massive database space savings. You save 1 template to send 1,000,000 customized messages.

### Step 3.2: Dynamic Variable Replacement
```java
String finalMessageContent = buildMessageFromTemplate(template.getContent(), requestDTO.getVariables());
// Example: Replaces {{name}} with "Artem" inside an iteration loop.
```

### Step 3.3: Dynamic Provider Selection (Plugin Architecture)
```java
BroadcastProviderPlugin selectedProvider = providers.stream()
        .filter(p -> p.getProviderName().equalsIgnoreCase(requestDTO.getProvider()))
        .findFirst()
// ...
WhatsAppProvider dbProvider = providerRepository.findByProviderNameIgnoreCaseAndStatus(selectedProvider.getProviderName(), ProviderStatus.ACTIVE)
```
**Why use a Plugin Pattern here?**
Rather than writing messy `if (provider == "infobip") { ... } else if (provider == "360dialog") { ... }` blocks, Spring Boot dynamically injects a `List<BroadcastProviderPlugin>`. This is the **Strategy Pattern**. If tomorrow you add "Twilio", you just create a new `TwilioProviderPlugin.java`. You **don't have to change a single line of code in the core service!**
It also verifies if the provider is currently allowed (`ProviderStatus.ACTIVE`) in the DB, giving Admins an instant "Kill Switch" to disable a failing provider.

### Step 3.4: The Actual API Call
```java
SendMessageResponseDTO response = selectedProvider.sendMessage(requestDTO.getMobileNumber(), finalMessageContent);
```
At this point, the code executes inside either [InfobipProviderPlugin.java](file:///home/artem/Desktop/Backend/Message_Broadcast/src/main/java/com/example/messagebroadcast/service/InfobipProviderPlugin.java) or [Dialog360ProviderPlugin.java](file:///home/artem/Desktop/Backend/Message_Broadcast/src/main/java/com/example/messagebroadcast/service/Dialog360ProviderPlugin.java).
- They both implement [BroadcastProviderPlugin](file:///home/artem/Desktop/Backend/Message_Broadcast/src/main/java/com/example/messagebroadcast/service/BroadcastProviderPlugin.java#5-14).
- They construct specific, distinct HTTP POST requests (Infobip uses `"Authorization": "App KEY"`, 360Dialog uses `"D360-API-KEY": "KEY"`).
- They parse the raw third-party JSON response to explicitly extract the `messageId` provided by WhatsApp.
- **Why?** They map complex, vendor-specific API responses into one clean, unified internal standard: [SendMessageResponseDTO](file:///home/artem/Desktop/Backend/Message_Broadcast/src/main/java/com/example/messagebroadcast/dto/SendMessageResponseDTO.java#7-16).

### Step 3.5: Robust Logging System
```java
WhatsAppLog messageLog = WhatsAppLog.builder()
        // ... sets mobile, template, provider, externalMessageId
        .build();
WhatsAppLog savedLog = logRepository.save(messageLog);

WhatsAppLogDetail detail = WhatsAppLogDetail.builder()
        .whatsAppLog(savedLog)
        .status(messageLog.getStatus()) // Step-by-step timeline history
        .build();
logDetailRepository.save(detail);
```
**Why the two tables ([WhatsAppLog](file:///home/artem/Desktop/Backend/Message_Broadcast/src/main/java/com/example/messagebroadcast/entity/WhatsAppLog.java#14-52) and [WhatsAppLogDetail](file:///home/artem/Desktop/Backend/Message_Broadcast/src/main/java/com/example/messagebroadcast/entity/WhatsAppLogDetail.java#12-39))?**
`tbWhatsAppLog` represents the **Macro view** (e.g., Message to +91...99; It was SENT). 
`tbWhatsAppLogDetail` represents the **Micro/Timeline view**. Because WhatsApp delivery happens over time, a message goes from `PENDING` -> `SENT` -> `DELIVERED` -> `READ`. Storing details recursively allows admins to trace the exact lifetime, retry counts, or debug deep failures linked to the exact parent message.

---

## 4. The Response Loop: Webhook Processing

Messaging isn't instant. When you send a message, Infobip says "Okay, I accepted it." Five seconds later, the telecom network might say "User's phone is off, Message Failed." 

To catch these asynchronous updates, you expose **Webhooks** (Passive, listening API endpoints).

### [WebhookController.java](file:///home/artem/Desktop/Backend/Message_Broadcast/src/main/java/com/example/messagebroadcast/controller/WebhookController.java) & [WebhookService.java](file:///home/artem/Desktop/Backend/Message_Broadcast/src/main/java/com/example/messagebroadcast/service/WebhookService.java)
```java
@PostMapping("/infobip")
public ResponseEntity<Void> handleInfobipWebhook(@RequestBody String payload) {
    // parses payload securely to find "messageId", "status", and "error"
    webhookService.updateStatus(messageId, status, error);
}
```

```java
// Inside WebhookService.java
Optional<WhatsAppLog> logEntryOpt = logRepository.findByExternalMessageId(externalMessageId);
logEntryOpt.get().setStatus(newStatus); // Update main historical log
// ...
WhatsAppLogDetail detail = WhatsAppLogDetail.builder().whatsAppLog(logEntry).build(); // Append to trace table
```
**Why is this architecture robust?**
1. **Third-Party Agnostic:** Webhooks hit distinct URLs (`/api/webhooks/infobip`), decode complex vendor JSON shapes, extract the standard UUID, and then call one unified processor ([updateStatus](file:///home/artem/Desktop/Backend/Message_Broadcast/src/main/java/com/example/messagebroadcast/service/WebhookService.java#22-44)).
2. **Event Sourcing Pattern:** Rather than overwriting the original log entirely (which loses history), we append a completely new row into [WhatsAppLogDetail](file:///home/artem/Desktop/Backend/Message_Broadcast/src/main/java/com/example/messagebroadcast/entity/WhatsAppLogDetail.java#12-39). This creates a flawless historical audit trail of *every single state change* that happened, giving incredible observability into telecom reliability!
