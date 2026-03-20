package com.example.messagebroadcast.controller;

import com.example.messagebroadcast.entity.WhatsAppLogDetail;
import com.example.messagebroadcast.repository.WhatsAppLogDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Map<String, Object>> getWhatsAppLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        try {
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "attemptAt"));
            Page<WhatsAppLogDetail> detailPage = logDetailRepository.findAllWithDetails(pageRequest);

            List<Map<String, Object>> logs = detailPage.getContent().stream().map(detail -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", detail.getLogDetailID());
                map.put("mobileNo", detail.getWhatsAppLog() != null ? detail.getWhatsAppLog().getMobileNo() : "N/A");
                map.put("template", detail.getWhatsAppLog() != null && detail.getWhatsAppLog().getTemplate() != null
                        ? detail.getWhatsAppLog().getTemplate().getName() : "N/A");
                map.put("provider", detail.getWhatsAppLog() != null && detail.getWhatsAppLog().getProvider() != null
                        ? detail.getWhatsAppLog().getProvider().getProviderName() : "N/A");
                map.put("status", detail.getStatus() != null ? detail.getStatus().name() : "UNKNOWN");
                map.put("errorMessage", detail.getErrorMessage() != null ? detail.getErrorMessage() : "");
                map.put("timestamp", detail.getAttemptAt());
                return map;
            }).collect(Collectors.toList());

            // Build paginated response
            Map<String, Object> response = new HashMap<>();
            response.put("logs", logs);
            response.put("currentPage", detailPage.getNumber());
            response.put("totalPages", detailPage.getTotalPages());
            response.put("totalItems", detailPage.getTotalElements());
            response.put("pageSize", detailPage.getSize());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> empty = new HashMap<>();
            empty.put("logs", List.of());
            empty.put("currentPage", 0);
            empty.put("totalPages", 0);
            empty.put("totalItems", 0);
            empty.put("pageSize", size);
            return ResponseEntity.ok(empty);
        }
    }

    @GetMapping("/sms")
    public ResponseEntity<Map<String, Object>> getSMSLogs() {
        Map<String, Object> empty = new HashMap<>();
        empty.put("logs", List.of());
        empty.put("currentPage", 0);
        empty.put("totalPages", 0);
        empty.put("totalItems", 0);
        empty.put("pageSize", 15);
        return ResponseEntity.ok(empty);
    }
}
