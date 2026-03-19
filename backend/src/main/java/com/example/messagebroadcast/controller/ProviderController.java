package com.example.messagebroadcast.controller;

import com.example.messagebroadcast.entity.WhatsAppProvider;
import com.example.messagebroadcast.service.ProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/providers")
@RequiredArgsConstructor
public class ProviderController {

    private final ProviderService providerService;

    @GetMapping
    public ResponseEntity<List<WhatsAppProvider>> getAllProviders() {
        return ResponseEntity.ok(providerService.getAllProviders());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateProviderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        
        String newStatus = payload.get("status");
        if (newStatus == null || (!newStatus.equals("ACTIVE") && !newStatus.equals("INACTIVE"))) {
            return ResponseEntity.badRequest().body("Invalid Status");
        }

        try {
            WhatsAppProvider updated = providerService.updateStatus(id, newStatus);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to update status: " + e.getMessage());
        }
    }
}
