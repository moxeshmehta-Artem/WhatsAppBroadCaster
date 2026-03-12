package com.example.messagebroadcast.service;

import com.example.messagebroadcast.dto.BroadcastRequestDTO;
import com.example.messagebroadcast.dto.SendMessageResponseDTO;
import com.example.messagebroadcast.entity.WhatsAppLog;
import com.example.messagebroadcast.entity.WhatsAppTemp;
import com.example.messagebroadcast.repository.WhatsAppLogRepository;
import com.example.messagebroadcast.repository.WhatsAppTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.messagebroadcast.repository.WhatsAppProviderRepository;
import com.example.messagebroadcast.entity.WhatsAppProvider;
import com.example.messagebroadcast.entity.WhatsAppLogDetail;
import com.example.messagebroadcast.repository.WhatsAppLogDetailRepository;


import java.util.List;
import java.util.Map;


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

        // 3.1 Fetch Provider Entity from DB (for normalization)
        WhatsAppProvider dbProvider = providerRepository.findByProviderNameIgnoreCase(selectedProvider.getProviderName())
                .orElseThrow(() -> new IllegalArgumentException("Provider not found in DB: " + selectedProvider.getProviderName()));

        // 4. Send Message via Provider API
        SendMessageResponseDTO response = selectedProvider.sendMessage(requestDTO.getMobileNumber(), finalMessageContent);

        // 5. Save details in DB Log
        WhatsAppLog messageLog = WhatsAppLog.builder()
                .mobileNo(requestDTO.getMobileNumber())
                .template(template)
                .provider(dbProvider)
                .status(response.isSuccess() ? "SENT" : "FAILED")
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
