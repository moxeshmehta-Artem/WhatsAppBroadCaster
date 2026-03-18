package com.example.messagebroadcast.repository;

import com.example.messagebroadcast.entity.WhatsAppLogDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WhatsAppLogDetailRepository extends JpaRepository<WhatsAppLogDetail, Long> {
}
