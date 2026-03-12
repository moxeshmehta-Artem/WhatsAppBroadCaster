package com.example.messagebroadcast.repository;

import com.example.messagebroadcast.entity.WhatsAppProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface WhatsAppProviderRepository extends JpaRepository<WhatsAppProvider, Long> {
    Optional<WhatsAppProvider> findByProviderNameIgnoreCase(String providerName);
}
