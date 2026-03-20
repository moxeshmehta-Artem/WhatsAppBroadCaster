package com.example.messagebroadcast.repository;

import com.example.messagebroadcast.entity.WhatsAppLogDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WhatsAppLogDetailRepository extends JpaRepository<WhatsAppLogDetail, Long> {
    
    // Fetch latest logs first for the dashboard
    List<WhatsAppLogDetail> findAllByOrderByAttemptAtDesc();
}
