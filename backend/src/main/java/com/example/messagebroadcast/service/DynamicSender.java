package com.example.messagebroadcast.service;

import com.example.messagebroadcast.dto.SendMessageResponseDTO;
import com.example.messagebroadcast.entity.WhatsAppProvider;
import com.example.messagebroadcast.enums.MessageStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class DynamicSender {

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Sends a message using a dynamic provider's configuration.
     */
    public SendMessageResponseDTO sendDynamicMessage(WhatsAppProvider provider, String mobile, String message) {
        try {
            // 1. Prepare JSON Template
            // Replace {{phone}} and {{message}} in the stored template
            String payload = provider.getPayloadTemplate()
                    .replace("{{phone}}", mobile)
                    .replace("{{message}}", message);

            // 2. Prepare Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Add Authorization header if API Key exists
            if (provider.getApiKey() != null && !provider.getApiKey().isEmpty()) {
                // Handle different auth schemes (Bearer vs API-Key)
                if (provider.getApiKey().toLowerCase().startsWith("bearer ") || 
                    provider.getApiKey().toLowerCase().startsWith("basic ")) {
                    headers.set("Authorization", provider.getApiKey());
                } else {
                    headers.set("Authorization", "Bearer " + provider.getApiKey());
                }
            }

            // 3. Send POST Request
            log.info("Sending dynamic broadcast via {}: {}", provider.getProviderName(), provider.getApiUrl());
            HttpEntity<String> entity = new HttpEntity<>(payload, headers);
            
            String response = restTemplate.postForObject(provider.getApiUrl(), entity, String.class);
            log.info("Dynamic response from {}: {}", provider.getProviderName(), response);

            return SendMessageResponseDTO.builder()
                    .status(MessageStatus.SENT)
                    .messageId("DYN-" + System.currentTimeMillis())
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Generic Provider Error ({}): {}", provider.getProviderName(), e.getMessage());
            return SendMessageResponseDTO.builder()
                    .status(MessageStatus.FAILED)
                    .success(false)
                    .errorDetails(e.getMessage())
                    .build();
        }
    }
}
