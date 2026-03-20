package com.example.messagebroadcast.service;

import com.example.messagebroadcast.entity.WhatsAppProvider;
import com.example.messagebroadcast.enums.ProviderStatus;
import com.example.messagebroadcast.repository.WhatsAppProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProviderService {

    private final WhatsAppProviderRepository providerRepository;

    public List<WhatsAppProvider> getAllProviders() {
        return providerRepository.findAll();
    }

    public WhatsAppProvider updateStatus(Long id, String status) {
        WhatsAppProvider provider = providerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Provider not found with ID: " + id));

        ProviderStatus newStatusEnum = ProviderStatus.valueOf(status);

        // --- SECURITY GUARD ---
        // If the user attempts to deactivate a provider, we must ensure another one is
        // still ACTIVE.
        if (newStatusEnum == ProviderStatus.INACTIVE && provider.getStatus() == ProviderStatus.ACTIVE) {
            long activeCount = providerRepository.findAll().stream()
                    .filter(p -> p.getStatus() == ProviderStatus.ACTIVE)
                    .count();

            if (activeCount <= 1) {
                throw new IllegalArgumentException(
                        "REJECTED: You cannot deactivate the last active provider. At-least one must remain active.");
            }
        }

        provider.setStatus(newStatusEnum);
        return providerRepository.save(provider);
    }

    public WhatsAppProvider createProvider(WhatsAppProvider provider) {
        // Enforce name uniqueness
        if (providerRepository.findAll().stream()
                .anyMatch(p -> p.getProviderName().equalsIgnoreCase(provider.getProviderName()))) {
            throw new IllegalArgumentException("Provider with name '" + provider.getProviderName() + "' already exists.");
        }
        
        provider.setStatus(ProviderStatus.ACTIVE);
        return providerRepository.save(provider);
    }
}
