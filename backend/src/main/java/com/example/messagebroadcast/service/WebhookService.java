package com.example.messagebroadcast.service;

import com.example.messagebroadcast.entity.WhatsAppLog;
import com.example.messagebroadcast.entity.WhatsAppLogDetail;
import com.example.messagebroadcast.repository.WhatsAppLogDetailRepository;
import com.example.messagebroadcast.repository.WhatsAppLogRepository;
import com.example.messagebroadcast.enums.MessageStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final WhatsAppLogRepository logRepository;
    private final WhatsAppLogDetailRepository logDetailRepository;

    @Transactional
    public void updateStatus(String externalMessageId, String statusStr, String errorMessage) {
        log.info("Updating status for message ID: {} to {}", externalMessageId, statusStr);
        
        // Map the string to our Enum safely
        MessageStatus newStatus = mapToEnum(statusStr);

        Optional<WhatsAppLog> logEntryOpt = logRepository.findByExternalMessageId(externalMessageId);

        if (logEntryOpt.isPresent()) {
            WhatsAppLog logEntry = logEntryOpt.get();
            logEntry.setStatus(newStatus);
            logRepository.save(logEntry);

            // Add detail entry for the status change
            WhatsAppLogDetail detail = WhatsAppLogDetail.builder()
                    .whatsAppLog(logEntry)
                    .status(newStatus)
                    .errorMessage(errorMessage != null ? errorMessage : "Status updated via Webhook: " + statusStr)
                    .build();
            logDetailRepository.save(detail);
        } else {
            log.warn("No WhatsApp log found for external message ID: {}", externalMessageId);
        }
    }

    private MessageStatus mapToEnum(String statusStr) {
        if (statusStr == null) return MessageStatus.SENT;
        
        String clean = statusStr.toUpperCase().trim();
        try {
            return MessageStatus.valueOf(clean);
        } catch (IllegalArgumentException e) {
            // Map common provider variants to our simplified enum
            if (clean.contains("FAIL") || clean.contains("REJECTED") || clean.contains("ERROR")) {
                return MessageStatus.FAILED;
            }
            return MessageStatus.SENT; // Default: provider accepted it
        }
    }
}
