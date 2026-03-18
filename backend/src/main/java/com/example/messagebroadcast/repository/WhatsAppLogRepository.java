package com.example.messagebroadcast.repository;

import com.example.messagebroadcast.entity.WhatsAppLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WhatsAppLogRepository extends JpaRepository<WhatsAppLog, Long> {
    Optional<WhatsAppLog> findByExternalMessageId(String externalMessageId);
}
