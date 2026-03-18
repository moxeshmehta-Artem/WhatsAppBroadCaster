package com.example.messagebroadcast.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.messagebroadcast.dto.SendMessageResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class InfobipProviderPlugin implements BroadcastProviderPlugin {

    @Value("${app.provider.infobip.api-key}")
    private String apiKey;

    @Value("${app.provider.infobip.base-url}")
    private String baseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public SendMessageResponseDTO sendMessage(String mobileNumber, String messageContent) {
        log.info("Sending message via Infobip to {}: {}", mobileNumber, messageContent);
        
        RestTemplate restTemplate = new RestTemplate();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "App " + apiKey); // Infobip specific auth

        // Construct Infobip WhatsApp text message payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("from", "447860088970"); // Your exact WhatsApp number from Infobip dashboard
        
        // Infobip WhatsApp API strictly requires country code WITHOUT the '+' (e.g. 917203820127)
        String cleanMobileNumber = mobileNumber.replaceAll("[^0-9]", "");
        if (cleanMobileNumber.length() == 10) {
            cleanMobileNumber = "91" + cleanMobileNumber; // Defaulting to India if user forgets Country Code
        }
        log.info("Parsed Recipient number for WhatsApp payload: " + cleanMobileNumber);
        
        payload.put("to", cleanMobileNumber);

        Map<String, Object> content = new HashMap<>();
        content.put("text", messageContent);
        payload.put("content", content);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            String url = baseUrl;
            // Ensure the URL always points to the official WhatsApp text API endpoint
            if (!url.endsWith("/whatsapp/1/message/text")) {
                url = url.endsWith("/") ? url + "whatsapp/1/message/text" : url + "/whatsapp/1/message/text";
            }
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            String messageId = null;
            try {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.has("messages") && root.get("messages").isArray() && root.get("messages").size() > 0) {
                    messageId = root.get("messages").get(0).get("messageId").asText();
                }
            } catch (Exception e) {
                log.warn("Failed to parse Infobip messageId: {}", e.getMessage());
            }

            return new SendMessageResponseDTO(true, "SENT_VIA_INFOBIP", messageId, "Response: " + response.getBody());
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error("API Error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return new SendMessageResponseDTO(false, "API_ERROR", null, e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Failed to send Infobip message: ", e);
            return new SendMessageResponseDTO(false, "FAILED_INFOBIP", null, e.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return "infobip";
    }
}
