# 🚀 Batch Processing — Complete Code Coverage

> This document covers every file involved in the **Batch Processing** system for the WhatsApp Bulk Broadcast feature. It explains the **problem**, the **solution**, the **flow**, and includes the **full source code** of every file touched.

---

## 📊 Performance Comparison

For a campaign targeting **5,000 contacts**:

| Operation               | ❌ Before (Per-Contact Loop) | ✅ After (Batch Processing) |
|--------------------------|-----------------------------|-----------------------------|
| Template DB Lookups      | 5,000                       | **1**                       |
| Provider DB Lookups      | 5,000                       | **1**                       |
| Provider Plugin Resolves | 5,000                       | **1**                       |
| Message Content Builds   | 5,000                       | **1**                       |
| `WhatsAppLog` INSERTs    | 5,000 (individual)          | **~10** (5000 ÷ 500)        |
| `WhatsAppLogDetail` INSERTs | 5,000 (individual)       | **~10** (5000 ÷ 500)        |
| **Total DB Calls**       | **~20,000**                 | **~24**                     |

---

## 🏗️ Architecture Overview

```
┌──────────────┐     ┌──────────────────────┐     ┌──────────────────┐
│   Angular    │────▶│  BroadcastController │────▶│  ExcelParser     │
│  (Frontend)  │     │  /api/broadcast/bulk │     │  Service         │
└──────────────┘     └───────┬──────────────┘     └──────────────────┘
                             │                         │ (returns List<String>)
                             ▼                         │
                    ┌──────────────────┐               │
                    │ BroadcastService │◀──────────────┘
                    │ processBulkBroadcast()            │
                    └────────┬─────────┘               │
                             │                         │
              ┌──────────────┼──────────────────┐
              │              │                  │
              ▼              ▼                  ▼
      ┌──────────┐   ┌────────────┐    ┌──────────────┐
      │ Phase 1  │   │  Phase 2   │    │   Phase 3    │
      │ ONE-TIME │   │  SEND +    │    │  FLUSH DB    │
      │ LOOKUPS  │   │  COLLECT   │    │  (per 500)   │
      └──────────┘   └────────────┘    └──────────────┘
       - Template      - API Call        - saveAll(logs)
       - Provider      - Build Log       - saveAll(details)
       - Message       - Track S/F       - Clear Batch
```

---

## 📁 Files Involved

| # | File | Layer | Purpose |
|---|------|-------|---------|
| 1 | `application.properties` | Config | Hibernate batch settings |
| 2 | `BulkBroadcastResult.java` | DTO | Response object for bulk results |
| 3 | `ExcelParserService.java` | Service | Parses Excel/CSV files + 10k limit |
| 4 | `BroadcastService.java` | Service | **Core batch processing logic** |
| 5 | `BroadcastController.java` | Controller | Thin controller, delegates to service |

---

## 📝 File 1: `application.properties`

**Path:** `src/main/resources/application.properties`

**What was added:**
- `hibernate.jdbc.batch_size=500` — Tells Hibernate to group up to 500 INSERT statements into a single JDBC call
- `hibernate.order_inserts=true` — Groups INSERT statements by entity type for maximum batching efficiency
- `hibernate.order_updates=true` — Same for UPDATE statements

```properties
spring.application.name=message-broadcast

# MySQL Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/message_broadcast_db?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=root123
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# --- BATCH PROCESSING: Reduces DB round trips for bulk inserts ---
spring.jpa.properties.hibernate.jdbc.batch_size=500
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl

# Provider Configurations
# Put your 360dialog API key here:
app.provider.360dialog.api-key=RT2K67LBD1R0Q58QMFZVD89DLUMUX3GS
app.provider.360dialog.base-url=https://waba-sandbox.360dialog.io/v1/messages

# Put your Infobip API key here:
app.provider.infobip.api-key=1220d6bc0ce0df5c73c7f8f21a3a1617-e29269c4-0f2e-494c-8ec6-917055ff471d
app.provider.infobip.base-url=https://1egpyk.api.infobip.com
```

### 💡 How `batch_size=500` works internally:

Without batching:
```sql
INSERT INTO tbWhatsAppLog (...) VALUES (...);   -- DB Call #1
INSERT INTO tbWhatsAppLog (...) VALUES (...);   -- DB Call #2
INSERT INTO tbWhatsAppLog (...) VALUES (...);   -- DB Call #3
-- ... 500 separate network round trips!
```

With `batch_size=500`:
```sql
INSERT INTO tbWhatsAppLog (...) VALUES (...), (...), (...), ... ;  -- 1 DB Call for 500 rows!
```

---

## 📝 File 2: `BulkBroadcastResult.java` (NEW)

**Path:** `src/main/java/com/example/messagebroadcast/dto/BulkBroadcastResult.java`

**Why created:** Instead of returning a raw `Map<>` from the controller (which is bad practice), we use a proper DTO. This gives us type safety and a clean JSON response.

```java
package com.example.messagebroadcast.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BulkBroadcastResult {
    private String status;          // "COMPLETED"
    private int totalSuccessful;    // e.g. 4800
    private int totalFailed;        // e.g. 200
    private String message;         // "Excel Blast Complete!"
}
```

**JSON Response sent to Angular:**
```json
{
  "status": "COMPLETED",
  "totalSuccessful": 4800,
  "totalFailed": 200,
  "message": "Excel Blast Complete!"
}
```

---

## 📝 File 3: `ExcelParserService.java`

**Path:** `src/main/java/com/example/messagebroadcast/service/ExcelParserService.java`

**What was added:** A **hard limit of 10,000 contacts** for both CSV and Excel files. If the file contains more than 10k numbers, parsing is immediately aborted with a clear error.

```java
package com.example.messagebroadcast.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ExcelParserService {

    public List<String> extractAllPhoneNumbers(MultipartFile file) {
        List<String> phoneNumbers = new ArrayList<>();
        
        try {
            if (file.getOriginalFilename() != null && file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(file.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] cols = line.split(",");
                        // Scan across the first 5 columns to find the phone number
                        for (String col : cols) {
                            String cleanedNumber = col.replaceAll("[^0-9]", "");
                            // A real phone number is usually between 10 to 15 digits!
                            if (cleanedNumber.length() >= 10 && cleanedNumber.length() <= 15) {
                                phoneNumbers.add(cleanedNumber);
                                
                                // SECURITY GUARD: Hard Limit of 10k contacts!
                                if (phoneNumbers.size() > 10000) {
                                    throw new IllegalArgumentException("REJECTED: CSV file exceeds the 10,000 contact limit. Please split it into smaller files.");
                                }
                                break; // Found it for this row! Move to next row.
                            }
                        }
                    }
                }
                log.info("Successfully extracted {} numbers from the CSV file!", phoneNumbers.size());
                return phoneNumbers;
            }

            // Otherwise, boot up Apache POI to parse binary .xlsx / .xls files
            try (InputStream is = file.getInputStream();
                 Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(is)) {
                
                log.info("Starting to read Excel workbook: {}", file.getOriginalFilename());
                
                Sheet sheet = workbook.getSheetAt(0);
                org.apache.poi.ss.usermodel.DataFormatter dataFormatter = new org.apache.poi.ss.usermodel.DataFormatter();
                
                for (Row row : sheet) {
                    if (row == null) continue;
                    
                    // Scan the first 5 columns across the row to intelligently find the phone number
                    for (int i = 0; i < 5; i++) {
                        org.apache.poi.ss.usermodel.Cell cell = row.getCell(i);
                        if (cell == null) continue;
                        
                        String rawNumber = "";
                        if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                            // Automatically blocks scientific notation data loss!
                            rawNumber = java.math.BigDecimal.valueOf(cell.getNumericCellValue()).toPlainString();
                        } else {
                            rawNumber = dataFormatter.formatCellValue(cell);
                        }
                        
                        String cleanedNumber = rawNumber.replaceAll("[^0-9]", "");
                        
                        // Valid Phone Numbers typically fall in the 10-15 character range.
                        if (cleanedNumber.length() >= 10 && cleanedNumber.length() <= 15) {
                            phoneNumbers.add(cleanedNumber);

                            // SECURITY GUARD: Hard Limit of 10k contacts!
                            if (phoneNumbers.size() > 10000) {
                                throw new IllegalArgumentException("REJECTED: Excel file exceeds the 10,000 contact limit. Please split it into smaller files.");
                            }
                            break; // We found the phone number for this row, stop scanning other columns!
                        }
                    }
                }
                
                log.info("Successfully extracted {} numbers from the Excel file!", phoneNumbers.size());
            }

        } catch (Exception e) {
            log.error("Failed to parse document", e);
            throw new RuntimeException("Could not read the document. Make sure it's a valid .xlsx or .csv file and the numbers are in the first column.");
        }
        
        return phoneNumbers;
    }
}
```

---

## 📝 File 4: `BroadcastService.java` ⭐ (Core Batch Logic)

**Path:** `src/main/java/com/example/messagebroadcast/service/BroadcastService.java`

**What was added:** The `processBulkBroadcast()` method — the heart of the batch processing system.

### How batch processing works (3 Phases):

```
PHASE 1 — ONE-TIME LOOKUPS (runs once, regardless of how many contacts)
  ├── Fetch Template from DB (1 query)
  ├── Resolve Provider Plugin from Spring context (1 stream filter)
  ├── Validate Provider is ACTIVE in DB (1 query)
  └── Build final message content from template (1 string operation)

PHASE 2 — BATCH SEND + COLLECT (runs for each contact)
  ├── Send message via provider API (Infobip/360Dialog)
  ├── Build WhatsAppLog entity (in memory, NOT saving to DB yet!)
  └── Track success/failure count

PHASE 3 — FLUSH TO DB (runs every 500 contacts)
  ├── saveAll(logBatch)     → Hibernate batches 500 INSERTs into ~1 DB call
  ├── saveAll(detailBatch)  → Hibernate batches 500 INSERTs into ~1 DB call
  └── Clear batch lists for next round
```

```java
package com.example.messagebroadcast.service;

import com.example.messagebroadcast.dto.BroadcastRequestDTO;
import com.example.messagebroadcast.dto.SendMessageResponseDTO;
import com.example.messagebroadcast.entity.WhatsAppLog;
import com.example.messagebroadcast.entity.WhatsAppTemp;
import com.example.messagebroadcast.entity.WhatsAppProvider;
import com.example.messagebroadcast.entity.WhatsAppLogDetail;
import com.example.messagebroadcast.enums.ProviderStatus;
import com.example.messagebroadcast.repository.WhatsAppLogRepository;
import com.example.messagebroadcast.repository.WhatsAppTemplateRepository;
import com.example.messagebroadcast.repository.WhatsAppProviderRepository;
import com.example.messagebroadcast.repository.WhatsAppLogDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.example.messagebroadcast.dto.BulkBroadcastResult;

@Service
@RequiredArgsConstructor
@Slf4j
public class BroadcastService {

    private final WhatsAppTemplateRepository templateRepository;
    private final WhatsAppLogRepository logRepository;
    private final List<BroadcastProviderPlugin> providers;
    private final WhatsAppProviderRepository providerRepository;
    private final WhatsAppLogDetailRepository logDetailRepository;

    // =====================================================================
    // SINGLE MESSAGE (used for Manual Number page — no batch needed)
    // =====================================================================
    public WhatsAppLog processAndBroadcast(BroadcastRequestDTO requestDTO) {
        log.info("Processing broadcast request for mobile {}, using provider {}", requestDTO.getMobileNumber(), requestDTO.getProvider());

        // 1. Fetch Template
        WhatsAppTemp template = templateRepository.findById(requestDTO.getTemplateId())
                .orElseThrow(() -> new IllegalArgumentException("Template not found with ID: " + requestDTO.getTemplateId()));

        // 2. Prepare Message Content (replace variables if any)
        String finalMessageContent = buildMessageFromTemplate(template.getContent(), requestDTO.getVariables());

        // 3. Select Provider
        BroadcastProviderPlugin selectedProvider = providers.stream()
                .filter(p -> p.getProviderName().equalsIgnoreCase(requestDTO.getProvider()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported provider: " + requestDTO.getProvider()));

        // 3.1 Fetch Provider Entity and check if it is active
        WhatsAppProvider dbProvider = providerRepository.findByProviderNameIgnoreCaseAndStatus(selectedProvider.getProviderName(), ProviderStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found or is INACTIVE: " + selectedProvider.getProviderName()));

        // 4. Send Message via Provider API
        SendMessageResponseDTO response = selectedProvider.sendMessage(requestDTO.getMobileNumber(), finalMessageContent);

        // 5. Save details in DB Log
        WhatsAppLog messageLog = WhatsAppLog.builder()
                .mobileNo(requestDTO.getMobileNumber())
                .template(template)
                .provider(dbProvider)
                .status(response.getStatus())
                .externalMessageId(response.getMessageId())
                .build();

        WhatsAppLog savedLog = logRepository.save(messageLog);

        // 6. Save Log Detail 
        WhatsAppLogDetail detail = WhatsAppLogDetail.builder()
                .whatsAppLog(savedLog)
                .status(messageLog.getStatus())
                .errorMessage(response.isSuccess() ? "Success" : response.getErrorDetails())
                .build();

        logDetailRepository.save(detail);

        return savedLog;
    }

    // =====================================================================
    // BULK BROADCAST — BATCH PROCESSED! (used for Excel Campaign page)
    // =====================================================================
    /**
     * BATCH PROCESSING: Process bulk broadcasts with minimal DB calls.
     * 
     * Optimizations:
     *  1. Template fetched ONCE (not N times)
     *  2. Provider fetched and validated ONCE (not N times)
     *  3. Provider plugin resolved ONCE (not N times)
     *  4. Message content built ONCE (not N times)
     *  5. DB inserts batched in groups of 500 using saveAll()
     */
    public BulkBroadcastResult processBulkBroadcast(
            List<String> phoneNumbers, String providerName, Long templateId, Map<String, String> variables) {

        // ========== PHASE 1: ONE-TIME LOOKUPS ==========
        
        // 1. Fetch Template ONCE
        WhatsAppTemp template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found with ID: " + templateId));

        // 2. Resolve Provider Plugin ONCE
        BroadcastProviderPlugin providerPlugin = providers.stream()
                .filter(p -> p.getProviderName().equalsIgnoreCase(providerName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported provider: " + providerName));

        // 3. Validate Provider is ACTIVE in DB ONCE
        WhatsAppProvider dbProvider = providerRepository.findByProviderNameIgnoreCaseAndStatus(providerPlugin.getProviderName(), ProviderStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found or is INACTIVE: " + providerPlugin.getProviderName()));

        // 4. Build the final message content ONCE (since all contacts get the same template)
        String finalMessageContent = buildMessageFromTemplate(template.getContent(), variables);

        log.info("Bulk Broadcast started: {} contacts, provider={}, template={}", phoneNumbers.size(), providerName, templateId);

        // ========== PHASE 2: BATCH SEND + COLLECT ==========
        
        int BATCH_SIZE = 500;
        int successCount = 0;
        int failedCount = 0;

        List<WhatsAppLog> logBatch = new ArrayList<>(BATCH_SIZE);
        List<WhatsAppLogDetail> detailBatch = new ArrayList<>(BATCH_SIZE);

        for (int i = 0; i < phoneNumbers.size(); i++) {
            String number = phoneNumbers.get(i);
            if (number == null || number.trim().isEmpty()) continue;

            try {
                // Send message via provider API
                SendMessageResponseDTO response = providerPlugin.sendMessage(number, finalMessageContent);

                // Build log entity (NOT saving yet!)
                WhatsAppLog messageLog = WhatsAppLog.builder()
                        .mobileNo(number)
                        .template(template)
                        .provider(dbProvider)
                        .status(response.getStatus())
                        .externalMessageId(response.getMessageId())
                        .build();

                logBatch.add(messageLog);

                // Track success/failure for the response
                if (response.getStatus() != null && (response.getStatus().contains("FAILED") || response.getStatus().contains("ERROR"))) {
                    failedCount++;
                } else {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("Failed to send message to {}: {}", number, e.getMessage());
                failedCount++;

                // Still log the failure in DB
                WhatsAppLog failedLog = WhatsAppLog.builder()
                        .mobileNo(number)
                        .template(template)
                        .provider(dbProvider)
                        .status("FAILED")
                        .build();
                logBatch.add(failedLog);
            }

            // ========== PHASE 3: FLUSH BATCH TO DB every 500 contacts ==========
            if (logBatch.size() >= BATCH_SIZE || i == phoneNumbers.size() - 1) {
                // Bulk save all logs in one DB call!
                List<WhatsAppLog> savedLogs = logRepository.saveAll(logBatch);

                // Build details for each saved log
                for (WhatsAppLog savedLog : savedLogs) {
                    WhatsAppLogDetail detail = WhatsAppLogDetail.builder()
                            .whatsAppLog(savedLog)
                            .status(savedLog.getStatus())
                            .errorMessage("FAILED".equals(savedLog.getStatus()) ? "Provider rejected or system error" : "Success")
                            .build();
                    detailBatch.add(detail);
                }

                // Bulk save all details in one DB call!
                logDetailRepository.saveAll(detailBatch);

                log.info("Batch flushed: {} logs saved. Progress: {}/{}", logBatch.size(), i + 1, phoneNumbers.size());

                // Clear batches for next round
                logBatch.clear();
                detailBatch.clear();
            }
        }

        log.info("Bulk Broadcast COMPLETE: {} success, {} failed", successCount, failedCount);

        return new BulkBroadcastResult("COMPLETED", successCount, failedCount, "Excel Blast Complete!");
    }

    // =====================================================================
    // HELPER: Template Variable Substitution
    // =====================================================================
    private String buildMessageFromTemplate(String content, Map<String, String> variables) {
        if (variables == null || variables.isEmpty()) {
            return content; // no variables to substitute
        }

        String result = content;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            // Replaces placeholder like {{name}}
            String placeholder = "\\{\\{" + entry.getKey() + "\\}\\}";
            result = result.replaceAll(placeholder, entry.getValue());
        }
        return result;
    }
}
```

---

## 📝 File 5: `BroadcastController.java`

**Path:** `src/main/java/com/example/messagebroadcast/controller/BroadcastController.java`

**What changed:** The `/bulk` endpoint is now clean — no business logic. It simply:
1. Extracts phone numbers from the uploaded file
2. Builds the variables map
3. Delegates everything to `BroadcastService.processBulkBroadcast()`

```java
package com.example.messagebroadcast.controller;

import com.example.messagebroadcast.dto.BroadcastRequestDTO;
import com.example.messagebroadcast.entity.WhatsAppLog;
import com.example.messagebroadcast.service.BroadcastService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/broadcast")
@RequiredArgsConstructor
public class BroadcastController {

    private final BroadcastService broadcastService;

    // Inject our new magical Excel tool!
    private final com.example.messagebroadcast.service.ExcelParserService excelParserService;

    // NEW ENDPOINT: Preview Excel Contacts
    @PostMapping("/preview")
    public ResponseEntity<?> previewExcelContacts(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            List<String> phoneNumbers = excelParserService.extractAllPhoneNumbers(file);
            return ResponseEntity.ok(phoneNumbers);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to parse Excel file: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> broadcastMessage(@RequestBody BroadcastRequestDTO requestDTO) {
        try {
            WhatsAppLog logResult = broadcastService.processAndBroadcast(requestDTO);
            return ResponseEntity.ok(logResult);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    // ENDPOINT for EXCEL Blasts (BATCH PROCESSED!)
    @PostMapping("/bulk")
    public ResponseEntity<?> bulkBroadcast(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam("provider") String provider,
            @RequestParam("templateId") Long templateId,
            @RequestParam("varDate") String varDate,
            @RequestParam("varAddress") String varAddress) {

        try {
            // 1. Extract all numbers from the Excel Sheet
            List<String> phoneNumbers = excelParserService.extractAllPhoneNumbers(file);

            // 2. Form the template variables map
            java.util.Map<String, String> variables = new java.util.HashMap<>();
            variables.put("Date", varDate);
            variables.put("address", varAddress);

            // 3. Delegate to Service Layer for BATCH PROCESSING!
            com.example.messagebroadcast.dto.BulkBroadcastResult result = 
                    broadcastService.processBulkBroadcast(phoneNumbers, provider, templateId, variables);

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Excel processing completely failed: " + e.getMessage());
        }
    }
}
```

---

## 🔑 Key Concepts Explained

### 1. Why `saveAll()` instead of `save()` in a loop?

```java
// ❌ BAD: N individual DB calls
for (WhatsAppLog log : logs) {
    logRepository.save(log);  // 1 INSERT = 1 network round trip to MySQL
}

// ✅ GOOD: 1 batched DB call for N entities
logRepository.saveAll(logs);  // Hibernate groups them into a SINGLE JDBC batch
```

When you call `saveAll()`, Hibernate internally uses **JDBC batch inserts**. Combined with the `batch_size=500` setting, it sends up to 500 rows in a single network call to MySQL.

### 2. Why are we collecting entities in a list before flushing?

```java
List<WhatsAppLog> logBatch = new ArrayList<>(500);  // Pre-allocated for 500

for (...) {
    logBatch.add(builtLog);     // Just add to memory (fast!)
    
    if (logBatch.size() >= 500) {
        logRepository.saveAll(logBatch);  // Flush to DB in bulk
        logBatch.clear();                 // Free memory for next batch
    }
}
```

This pattern is called **"Collect → Flush → Clear"**. It prevents:
- **Memory overload** (we don't keep all 10k entities in memory at once)
- **Excessive DB calls** (we only hit the DB every 500 contacts)

### 3. Why `order_inserts=true` in properties?

Without it, Hibernate might interleave INSERT statements for different entity types:
```sql
INSERT INTO tbWhatsAppLog ...
INSERT INTO tbWhatsAppLogDetails ...   -- Different table! Breaks the batch.
INSERT INTO tbWhatsAppLog ...
INSERT INTO tbWhatsAppLogDetails ...
```

With `order_inserts=true`, Hibernate groups them by entity type:
```sql
INSERT INTO tbWhatsAppLog ...          -- All logs together
INSERT INTO tbWhatsAppLog ...
INSERT INTO tbWhatsAppLogDetails ...   -- All details together
INSERT INTO tbWhatsAppLogDetails ...
```

This allows JDBC to batch them efficiently.

### 4. Why ONE-TIME lookups matter so much?

In the old code, **every single contact** triggered:
```java
// These 3 lines ran 5,000 times for 5,000 contacts!
WhatsAppTemp template = templateRepository.findById(templateId)...   // DB HIT
BroadcastProviderPlugin plugin = providers.stream().filter(...)...   // CPU scan
WhatsAppProvider dbProvider = providerRepository.findBy...(...)...   // DB HIT
```

But the template and provider are **the same for every contact** in a campaign! So we fetch them once and reuse the Java objects.

---

## 🔄 Complete Request Flow

```
1. User clicks "Launch WhatsApp Blast" in Angular
         │
2. Angular sends POST /api/broadcast/bulk
   with file + provider + templateId + variables
         │
3. BroadcastController receives the multipart request
         │
4. ExcelParserService.extractAllPhoneNumbers()
   ├── Parses CSV or XLSX file
   ├── Extracts phone numbers (10-15 digits)
   └── Enforces 10,000 contact LIMIT
         │
5. BroadcastService.processBulkBroadcast()
   │
   ├── PHASE 1: Fetch template, provider, build message (ONCE)
   │
   ├── PHASE 2: Loop through each contact
   │   ├── Send message via Infobip/360Dialog API
   │   ├── Build WhatsAppLog entity (in memory)
   │   └── Track success/failure
   │
   └── PHASE 3: Every 500 contacts
       ├── logRepository.saveAll(batch)        → ~1 DB call
       ├── logDetailRepository.saveAll(batch)  → ~1 DB call
       └── Clear batch, continue
         │
6. Return BulkBroadcastResult to Angular
         │
7. Angular shows: "Successfully Delivered: 4800 | Failed: 200"
```

---

## ⚠️ Important Notes

1. **Restart the Spring Boot backend** after these changes — the Hibernate batch settings only take effect on startup.
2. The **10,000 contact limit** is enforced at the parsing stage, before any messages are sent.
3. The **batch size of 500** is a balanced choice — large enough to minimize DB round trips, small enough to avoid memory issues.
4. Individual messages (from the "Manual Number" page) still use the original `processAndBroadcast()` method — batch processing only applies to Excel campaigns.
