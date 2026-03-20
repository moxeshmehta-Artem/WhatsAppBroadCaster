package com.example.messagebroadcast.controller;

import com.example.messagebroadcast.entity.WhatsAppLogDetail;
import com.example.messagebroadcast.repository.WhatsAppLogDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final WhatsAppLogDetailRepository logDetailRepository;

    @GetMapping("/whatsapp")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getWhatsAppLogs() {
        List<WhatsAppLogDetail> details = logDetailRepository.findAllByOrderByAttemptAtDesc();
        
        // Transform to a clean JSON structure for the frontend
        List<Map<String, Object>> response = details.stream().map(detail -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", detail.getLogDetailID());
            map.put("mobileNo", detail.getWhatsAppLog().getMobileNo());
            map.put("template", detail.getWhatsAppLog().getTemplate().getName());
            map.put("provider", detail.getWhatsAppLog().getProvider().getProviderName());
            map.put("status", detail.getStatus());
            map.put("errorMessage", detail.getErrorMessage() != null ? detail.getErrorMessage() : "");
            map.put("timestamp", detail.getAttemptAt());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/sms")
    public ResponseEntity<List<Map<String, Object>>> getSMSLogs() {
        // Placeholder for SMS logs as requested by the toggle
        return ResponseEntity.ok(List.of());
    }
}
