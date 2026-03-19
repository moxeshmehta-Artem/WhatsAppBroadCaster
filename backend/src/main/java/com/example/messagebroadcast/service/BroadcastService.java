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
import com.example.messagebroadcast.enums.MessageStatus;
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

    public WhatsAppLog processAndBroadcast(BroadcastRequestDTO requestDTO) {
        log.info("Processing broadcast request for mobile {}, using provider {}", requestDTO.getMobileNumber(), requestDTO.getProvider());

        // 1. Fetch Template
        WhatsAppTemp template = templateRepository.findById(requestDTO.getTemplateId())
                .orElseThrow(() -> new IllegalArgumentException("Template not found with ID: " + requestDTO.getTemplateId()));

        // 2. Prepare Message Content (replace variables if any)
        String finalMessageContent = buildMessageFromTemplate(template.getContent(), requestDTO.getVariables());

        // 3. Select Provider

        //Keep it in a factory resolver following SRP
        BroadcastProviderPlugin selectedProvider = providers.stream()
                .filter(p -> p.getProviderName().equalsIgnoreCase(requestDTO.getProvider()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported provider: " + requestDTO.getProvider()));

        // 3.1 Fetch Provider Entity and check if it is active
        WhatsAppProvider dbProvider = providerRepository.findByProviderNameIgnoreCaseAndStatus(selectedProvider.getProviderName(), ProviderStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found or is INACTIVE: " + selectedProvider.getProviderName()));

        // 4. Send Message via Provider API
        SendMessageResponseDTO response = selectedProvider.sendMessage(requestDTO.getMobileNumber(), finalMessageContent);

        // 5. Save details   in DB Log
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
                if (response.getStatus() == MessageStatus.FAILED || response.getStatus() == MessageStatus.ERROR) {
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
                        .status(MessageStatus.FAILED)
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
                            .errorMessage(savedLog.getStatus() == MessageStatus.FAILED ? "Provider rejected or system error" : "Success")
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
