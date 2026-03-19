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

    // NEW ENDPOINT for EXCEL Blasts!
    @PostMapping("/bulk")
    public ResponseEntity<?> bulkBroadcast(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam("provider") String provider,
            @RequestParam("templateId") Long templateId,
            @RequestParam("varDate") String varDate,
            @RequestParam("varAddress") String varAddress) {

        System.out.println("Processing Excel file uploaded by Frontend...");
        
        try {
            // 1. Extract all numbers from the Excel Sheet
            List<String> phoneNumbers = excelParserService.extractAllPhoneNumbers(file);
            System.out.println("Found " + phoneNumbers.size() + " Numbers in the Document!");

            // 2. Form the template variables map
            java.util.Map<String, String> variables = new java.util.HashMap<>();
            variables.put("Date", varDate);
            variables.put("address", varAddress);

            // 3. Loop through every single number and blast the message!
            int successCount = 0;
            int failedCount = 0;
            
            for (String number : phoneNumbers) {
                // Skip completely invalid phone numbers just to be perfectly safe
                if (number == null || number.trim().isEmpty()) continue;
                
                BroadcastRequestDTO dto = new BroadcastRequestDTO();
                dto.setMobileNumber(number);
                dto.setProvider(provider);
                dto.setTemplateId(templateId);
                dto.setVariables(variables);
                
                // Fire the message!
                try {
                   WhatsAppLog log = broadcastService.processAndBroadcast(dto);
                   
                   // Check the literal database status to verify if Infobip rejected it!
                   if (log.getStatus() != null && (log.getStatus().contains("FAILED") || log.getStatus().contains("ERROR"))) {
                       failedCount++;
                   } else {
                       successCount++;
                   }
                   
                } catch(IllegalArgumentException e) {
                   // This specifically catches the "Provider is INACTIVE" or "Template Missing" errors
                   // We want to abort the ENTIRE loop instantly and tell the user, instead of running 10,000 times!
                   return ResponseEntity.badRequest().body(e.getMessage());
                } catch(Exception e) {
                   System.err.println("Database/System crash sending to " + number + " inside bulk loop: " + e.getMessage());
                   failedCount++;
                }
            }

            // Report the EXACT facts back to Angular!
            return ResponseEntity.ok(java.util.Map.of(
                "status", "COMPLETED",
                "totalSuccessful", successCount,
                "totalFailed", failedCount,
                "message", "Excel Blast Complete!"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Excel processing completely failed: " + e.getMessage());
        }
    }
}
