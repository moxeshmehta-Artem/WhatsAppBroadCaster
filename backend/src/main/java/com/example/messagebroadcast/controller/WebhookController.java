package com.example.messagebroadcast.controller;

import com.example.messagebroadcast.service.WebhookService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final WebhookService webhookService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/infobip")
    public ResponseEntity<Void> handleInfobipWebhook(@RequestBody String payload) {
        log.info("Received Infobip Webhook: {}", payload);
        try {
            JsonNode root = objectMapper.readTree(payload);
            if (root.has("results") && root.get("results").isArray()) {
                for (JsonNode result : root.get("results")) {
                    String messageId = result.get("messageId").asText();
                    String status = result.get("status").get("name").asText();
                    String error = result.has("error") ? result.get("error").toString() : null;
                    webhookService.updateStatus(messageId, status, error);
                }
            }
        } catch (Exception e) {
            log.error("Error processing Infobip webhook", e);
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/360dialog")
    public ResponseEntity<Void> handle360DialogWebhook(@RequestBody String payload) {
        log.info("Received 360Dialog Webhook: {}", payload);
        try {
            JsonNode root = objectMapper.readTree(payload);
            if (root.has("statuses") && root.get("statuses").isArray()) {
                for (JsonNode statusNode : root.get("statuses")) {
                    String messageId = statusNode.get("id").asText();
                    String status = statusNode.get("status").asText();
                    String error = statusNode.has("errors") ? statusNode.get("errors").toString() : null;
                    webhookService.updateStatus(messageId, status.toUpperCase(), error);
                }
            }
        } catch (Exception e) {
            log.error("Error processing 360Dialog webhook", e);
        }
        return ResponseEntity.ok().build();
    }
}
