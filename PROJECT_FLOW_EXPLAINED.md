# 🚀 WhatsApp Broadcast System: Simple Explanation

Imagine you run a massive **Delivery Company** (like FedEx or Amazon). Your job is to take a message, put it in a nice envelope, and make sure it reaches thousands of people.

Here is how our project works, explained like a story!

---

## 1. The Request (The Customer Order)
The flow starts when a "Customer" (a user of our app) says: 
> *"Hey, send this 'Order Confirmed' message to this phone number: +91 9876543210 using the Infobip courier."*

**Code Location:** `BroadcastController.java`
- This is the **Front Desk**. It receives the order, checks if it looks okay, and hands it over to the manager.

---

## 2. The Dispatcher (The Brain)
The Manager (Dispatcher) takes the order and does a few things:
1. **Find the Blueprint:** It goes to the filing cabinet (Database) and finds the "Order Confirmed" template.
2. **Fill in the Blanks:** The template says: *"Hi {{name}}, your order #{{id}} is ready!"*. The Manager replaces `{{name}}` with "Arjun" and `{{id}}` with "123".
3. **Pick the Courier:** The Manager sees the request asked for "Infobip". It goes to the Garage and finds the **Infobip Truck**.

**Code Location:** `BroadcastService.java`
- Method: `processAndBroadcast()`
- This is the heart of the system. It coordinates the template, the data, and the delivery.

---

## 3. The Courier Plugins (The Delivery Trucks)
We have different trucks for different companies (some use **Infobip**, some use **360Dialog**). 
Each truck speaks a different language:
- **Infobip Truck:** Wants the phone number formatted without the `+`.
- **360Dialog Truck:** Wants the data in a slightly different box.

The truck takes the letter, drives to the recipient's house (the WhatsApp API), and drops it off. It comes back with a "Tracking ID" (Message ID).

**Code Location:** `InfobipProviderPlugin.java` & `Dialog360ProviderPlugin.java`
- These files handle the "nitty-gritty" details of talking to external APIs.

---

## 4. The Delivery Logbook (Keeping Records)
The Manager wants to remember everything. 
1. **The Main Log (`WhatsAppLog`):** A big book that says: *"Sent 'Order Confirmed' to Arjun via Infobip. Tracking ID: INF-567. Current Status: SENT."*
2. **The Detail Log (`WhatsAppLogDetail`):** A more detailed notebook that records every single step. *"Time 10:00 AM: Message handed to Infobip. Time 10:05 AM: Infobip says 'Success'."*

**Code Location:** `WhatsAppLog.java` and `WhatsAppLogDetail.java`

---

## 5. The Webhook (The Delivery Confirmation)
A few minutes later, the Courier (Infobip) sends us a notification: 
> *"Hey! Message INF-567 was just DELIVERED!"*

**Code Location:** `WebhookController.java`
- This is like a **Post Box** on our building where couriers drop off status updates.

---

## 6. The Status Update (Closing the Loop)
Our system sees the note in the Post Box, finds the entry `INF-567` in the Logbook, and updates the status from `SENT` to `DELIVERED`.

**Code Location:** `WebhookService.java`
- It updates the records so the customer can see that their message actually reached the person.

---

### Summary of the "Golden Path"
1. **Controller** (Front Desk) receives request.
2. **Service** (Manager) fetches template & fills blanks.
3. **Plugin** (Truck) sends to WhatsApp & gets Tracking ID.
4. **Database** (Logbook) saves the "Sent" status.
5. **Webhook** (Notification) receives "Delivered" update.
6. **WebhookService** (Clerk) updates the status to "Delivered".

---

## 🛠️ Technical Deep Dive: Code-by-Code

For the coders (or curious minds), here is exactly what happens in the code:

### 1. `BroadcastController.java`
- **What it does:** It provides a REST API endpoint `@PostMapping`.
- **Key Logic:** It takes the incoming JSON data (which is converted into `BroadcastRequestDTO`) and passes it straight to the `broadcastService`.
- **Safety:** It has a `try-catch` block to return helpful error messages (like `400 Bad Request` if something is wrong with the data).

### 2. `BroadcastService.java`
- **`processAndBroadcast` method:**
    - **Step 1:** Queries the `templateRepository` to get the message format.
    - **Step 2:** Calls `buildMessageFromTemplate`. This uses a simple `for` loop to look for strings like `{{name}}` and replace them with actual values using `result.replaceAll(placeholder, value)`.
    - **Step 3 (Selection):** It uses a **Factory Pattern** approach. It looks at all available "Plugins" (Infobip, 360Dialog) and picks the one that matches the name in the request.
    - **Step 4 (Dispatch):** It calls `selectedProvider.sendMessage()`. This returns a `SendMessageResponseDTO`, which contains the `messageId` from the provider.
    - **Step 5 (Logging):** It saves the transaction in `WhatsAppLog`. Note that it stores the **Template ID** and **Provider ID** (normalization), not just the text, making the database very efficient.

### 3. `InfobipProviderPlugin.java`
- **What it does:** Handles the actual HTTP call to Infobip.
- **Key Logic:** 
    - It uses Spring's `RestTemplate` to send a `POST` request.
    - It sets a special `Authorization` header required by Infobip.
    - It includes a "Phone Number Normalizer" (`replaceAll("[^0-9]", "")`) because WhatsApp APIs are very picky about phone number formats.
    - It parses the response JSON to extract the `messageId`.

### 4. `WebhookController.java`
- **What it does:** Listens for "Webhooks". A Webhook is just a URL that external services (like Infobip) call when they have an update.
- **Key Logic:**
    - It has different methods for different providers because their JSON formats are different.
    - It extracts the `messageId` and the new `status` (like "DELIVERED" or "READ").

### 5. `WebhookService.java`
- **What it does:** Finalizes the record update.
- **Key Logic:**
    - It searches for the original log entry using `logRepository.findByExternalMessageId(messageId)`.
    - It updates the status in the main table.
    - It creates a **New Detail Record** in `WhatsAppLogDetail`. This is important because it creates a "Status History" (e.g., you can see exactly when it went from Sent to Delivered).

**That's it! Your messages are flying safe and tracked! 🕊️**
