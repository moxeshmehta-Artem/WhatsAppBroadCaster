package com.example.messagebroadcast.service;

import com.example.messagebroadcast.entity.WhatsAppLog;
import com.example.messagebroadcast.entity.WhatsAppLogDetail;
import com.example.messagebroadcast.repository.WhatsAppLogDetailRepository;
import com.example.messagebroadcast.repository.WhatsAppLogRepository;
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
    public void updateStatus(String externalMessageId, String newStatus, String errorMessage) {
        log.info("Updating status for message ID: {} to {}", externalMessageId, newStatus);

        Optional<WhatsAppLog> logEntryOpt = logRepository.findByExternalMessageId(externalMessageId);

        if (logEntryOpt.isPresent()) {
            WhatsAppLog logEntry = logEntryOpt.get();
            logEntry.setStatus(newStatus);
            logRepository.save(logEntry);

            // Add detail entry for the status change
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
}
