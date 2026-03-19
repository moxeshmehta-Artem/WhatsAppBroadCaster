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

    // ENDPOINT for EXCEL Blasts (FULLY DYNAMIC!)
    @PostMapping("/bulk")
    public ResponseEntity<?> bulkBroadcast(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam("provider") String provider,
            @RequestParam("templateId") Long templateId,
            @RequestParam java.util.Map<String, String> allRequestParams) {

        try {
            // 1. Extract all numbers from the Excel Sheet
            List<String> phoneNumbers = excelParserService.extractAllPhoneNumbers(file);

            // 2. Identify variables dynamically (any param that isn't provider or templateId)
            java.util.Map<String, String> variables = new java.util.HashMap<>(allRequestParams);
            variables.remove("provider");
            variables.remove("templateId");
            // Note: 'file' is handled by MultipartFile, so it's not in the map anyway.

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
