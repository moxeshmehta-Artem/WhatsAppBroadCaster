package com.example.messagebroadcast.repository;

import com.example.messagebroadcast.entity.WhatsAppTemp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WhatsAppTemplateRepository extends JpaRepository<WhatsAppTemp, Long> {
    
    // For Uniqueness check!
    boolean existsByName(String name);

    // Optional: Find by name
    Optional<WhatsAppTemp> findByName(String name);
}
