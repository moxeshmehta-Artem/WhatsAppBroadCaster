package com.example.messagebroadcast.repository;

import com.example.messagebroadcast.entity.WhatsAppTemp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WhatsAppTemplateRepository extends JpaRepository<WhatsAppTemp, Long> {
}
