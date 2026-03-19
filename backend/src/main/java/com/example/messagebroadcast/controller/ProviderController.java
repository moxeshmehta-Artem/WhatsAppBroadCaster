package com.example.messagebroadcast.controller;

import com.example.messagebroadcast.entity.WhatsAppProvider;
import com.example.messagebroadcast.enums.ProviderStatus;
import com.example.messagebroadcast.repository.WhatsAppProviderRepository;
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

    private final WhatsAppProviderRepository providerRepository;

    @GetMapping
    public ResponseEntity<List<WhatsAppProvider>> getAllProviders() {
        return ResponseEntity.ok(providerRepository.findAll());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateProviderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        
        WhatsAppProvider provider = providerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Provider not found"));
                
        String newStatus = payload.get("status");
        if (newStatus == null || (!newStatus.equals("ACTIVE") && !newStatus.equals("INACTIVE"))) {
            return ResponseEntity.badRequest().body("Invalid Status");
        }
        
        provider.setStatus(ProviderStatus.valueOf(newStatus));
        WhatsAppProvider updated = providerRepository.save(provider);
        
        return ResponseEntity.ok(updated);
    }
}
