# Deep Dive: Message Broadcast Architecture & Code Flow

This document provides an **in-depth, code-by-code explanation** of the Message Broadcast System. It explicitly details the execution flow from the moment a user makes an API request to the moment the database saves the log, answering *how* and *why* each component operates.

---

## 1. The Core Architectural Concepts (The "Why")

Before walking through the code, it is important to understand the two core engineering principles used in this application:

### A. The Strategy Design Pattern
**The Problem:** If you want to support both Infobip and 360dialog, the standard beginner approach is to write a massive `if-else` statement inside your service class: `if (provider == "infobip") { ... } else if (provider == "360dialog") { ... }`. As you add Twilio, Gupshup, or WhatsApp Cloud, this file becomes thousands of lines long and impossible to maintain.
**The Solution:** We implemented the **Strategy Pattern** via the `BroadcastProviderPlugin` interface. The core Service logic never cares *how* a message is sent. It simply finds the correct plugin and says, `.sendMessage()`. Each plugin perfectly isolates the unique API headers, JSON structure, and URL for its respective company. 

### B. Synchronous API Invocation vs Asynchronous Delivery
**The Problem:** You asked, *"Why did it say 'SENT' even when my phone didn't receive it?"* 
**The Solution:** This architecture currently tracks the **Synchronous API Invocation**. 
When our Java code sends an HTTP request to Infobip, Infobip responds in milliseconds saying `"HTTP 200 OK - I received your request"`. Our Java code interprets this as `"SENT"`. However, the actual text message delivery across telecom networks can take seconds, minutes, or completely fail due to account restrictions (like Free Trial unverified numbers). To track absolute delivery to the handset, a separate Webhook architecture would be required to listen asynchronously for Infobip's status updates.

---

## 2. Code-by-Code Execution Flow

When a POST request hits `http://localhost:8080/api/broadcast`, the execution strictly follows this path:

### Step 1: Request Reception
**File:** `controller/BroadcastController.java`
**File:** `dto/BroadcastRequestDTO.java`

1. Postman sends a JSON body payload.
2. Spring Boot maps the JSON exactly into the properties of the `BroadcastRequestDTO` (e.g., `mobileNumber`, `templateId`, `provider`, `variables`).
3. The `@RestController` receives this DTO mapped by `@RequestBody`.
4. The Controller immediately passes this DTO to the Business Logic layer: `broadcastService.processAndBroadcast(requestDTO)`.

### Step 2: Template Retrieval from Database
**File:** `service/BroadcastService.java`
**File:** `repository/MessageTemplateRepository.java`

1. Inside `processAndBroadcast`, the Service asks the Database for the template: `templateRepository.findById(requestDTO.getTemplateId())`.
2. Hibernate/JPA executes a `SELECT * FROM message_template WHERE id = ?`.
3. The Database returns the `MessageTemplate` Java Object representing row ID 1: `"Gently,\n There is a medical camp of Dr.Vora,\n it will arrange it on {{Date}} at {{address}}.\n Thank You."`

### Step 3: Dynamic Variable Injection
**File:** `service/BroadcastService.java` (Method: `buildMessageFromTemplate`)

1. The Service looks at the `Map<String, String> variables` provided in your JSON request (e.g., `{"Date": "19th August", "address": "Thaltej"}`).
2. It loops through those variables and uses Regex `.replaceAll("\\{\\{" + key + "\\}\\}", value)` to find the double curly braces and overwrite them.
3. The raw string is finalized into the exact text message that must be sent.

### Step 4: Provider Selection (Strategy Routing)
**File:** `service/BroadcastService.java`
**File:** `service/BroadcastProviderPlugin.java`

1. Spring Boot automatically injects `List<BroadcastProviderPlugin> providers` into the `BroadcastService`. This list contains *every* active plugin in the codebase (`InfobipProviderPlugin`, `Dialog360ProviderPlugin`, etc.).
2. The Service streams this list, checking `.getProviderName().equalsIgnoreCase(requestDTO.getProvider())`.
3. Because you passed `"provider": "infobip"`, it selects the `InfobipProviderPlugin` instance.

### Step 5: Third-Party API Execution
**File:** `service/InfobipProviderPlugin.java`
**File:** `config/application.properties`

1. The `BroadcastService` triggers: `selectedProvider.sendMessage(mobile, finalMessageContent)`.
2. `InfobipProviderPlugin` wakes up. It reads `@Value("${app.provider.infobip.api-key}")` from `application.properties` to get the secret authorization key.
3. It cleans the mobile number. Because WhatsApp's API is incredibly strict, it removes all non-numeric characters and forcefully prepends `"91"` if you only passed a 10-digit Indian mobile number.
4. It manually constructs the exact nested `HashMap` JSON payload structure required by Infobip's documentation (including the hardcoded `"from": "447860088970"` WhatsApp business number).
5. It uses Spring's `RestTemplate` to send an `HTTP POST` Request across the internet to `https://1egpyk.api.infobip.com/whatsapp/1/message/text`.
6. Infobip immediately replies `HTTP 200 OK`. The Plugin catches this and returns a standard internal `SendMessageResponseDTO(true, "SENT_VIA_INFOBIP", ...)`.

### Step 6: Database Audit Logging
**File:** `service/BroadcastService.java`
**File:** `repository/MessageLogRepository.java`

1. The `BroadcastService` receives the response DTO from the plugin.
2. It needs to permanently record this action. It uses `MessageLog.builder()` to create a pristine audit entity containing:
   - The cleaned mobile number
   - The final injected message text
   - The Template object used
   - The literal string `"INFOBIP"` 
   - The String `"SENT"` (because the API call didn't throw an HTTP 500 error).
3. It calls `logRepository.save(messageLog)`.
4. Hibernate/JPA executes an `INSERT INTO message_log (mobile_number, message_content, template_id, provider, status) VALUES (...)`. MySQL auto-generates the `id` and `created_at` timestamp.

### Step 7: Final HTTP Response
**File:** `controller/BroadcastController.java`

1. The `BroadcastService` returns that successfully saved `MessageLog` object back up to the Controller.
2. The Controller wraps it in `ResponseEntity.ok(logResult)`.
3. Spring Boot converts the Java Database Entity back into pure JSON and sends it over the network.
4. Postman displays the `200 OK` status and the JSON object confirming the log ID, the timestamp, and the `SENT` status.
