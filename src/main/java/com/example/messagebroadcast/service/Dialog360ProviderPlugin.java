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
public class Dialog360ProviderPlugin implements BroadcastProviderPlugin {

    @Value("${app.provider.360dialog.api-key}")
    private String apiKey;

    @Value("${app.provider.360dialog.base-url}")
    private String baseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public SendMessageResponseDTO sendMessage(String mobileNumber, String messageContent) {
        log.info("Sending message via 360dialog to {}: {}", mobileNumber, messageContent);
        
        RestTemplate restTemplate = new RestTemplate();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("D360-API-KEY", apiKey); // Standard 360dialog auth header

        // 360dialog WhatsApp API strictly requires country code WITHOUT the '+' (e.g. 917203820127)
        String cleanMobileNumber = mobileNumber.replaceAll("[^0-9]", "");
        if (cleanMobileNumber.length() == 10) {
            cleanMobileNumber = "91" + cleanMobileNumber; // Defaulting to India if user forgets Country Code
        }
        log.info("Parsed Recipient number for 360dialog WhatsApp payload: " + cleanMobileNumber);

        // Construct 360dialog matching payload. 
        // 360dialog typically uses WhatsApp Business API structure.
        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", cleanMobileNumber);
        payload.put("type", "text");
        
        Map<String, String> textNode = new HashMap<>();
        textNode.put("body", messageContent);
        payload.put("text", textNode);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, request, String.class);
            
            String messageId = null;
            try {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.has("messages") && root.get("messages").isArray() && root.get("messages").size() > 0) {
                    messageId = root.get("messages").get(0).get("id").asText();
                }
            } catch (Exception e) {
                log.warn("Failed to parse 360dialog messageId: {}", e.getMessage());
            }

            return new SendMessageResponseDTO(true, "SENT_VIA_360DIALOG", messageId, "Response: " + response.getBody());
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error("Failed to send 360dialog message. HTTP STATUS: " + e.getStatusCode() + " BODY: " + e.getResponseBodyAsString());
            return new SendMessageResponseDTO(false, "FAILED_360DIALOG", null, "HTTP " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Failed to send 360dialog message: ", e);
            return new SendMessageResponseDTO(false, "FAILED_360DIALOG", null, e.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return "360dialog";
    }
}
