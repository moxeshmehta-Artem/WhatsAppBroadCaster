package com.example.messagebroadcast.repository;

import com.example.messagebroadcast.entity.WhatsAppProvider;
import com.example.messagebroadcast.enums.ProviderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface WhatsAppProviderRepository extends JpaRepository<WhatsAppProvider, Long> {
    Optional<WhatsAppProvider> findByProviderNameIgnoreCase(String providerName);
    Optional<WhatsAppProvider> findByProviderNameIgnoreCaseAndStatus(String providerName, ProviderStatus status);
}
