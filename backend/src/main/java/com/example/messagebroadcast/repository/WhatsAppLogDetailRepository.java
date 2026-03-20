package com.example.messagebroadcast.repository;

import com.example.messagebroadcast.entity.WhatsAppLogDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WhatsAppLogDetailRepository extends JpaRepository<WhatsAppLogDetail, Long> {
    
    // Paginated query with JOIN FETCH for eager loading
    @Query(value = "SELECT d FROM WhatsAppLogDetail d " +
           "JOIN FETCH d.whatsAppLog l " +
           "JOIN FETCH l.template " +
           "JOIN FETCH l.provider",
           countQuery = "SELECT COUNT(d) FROM WhatsAppLogDetail d")
    Page<WhatsAppLogDetail> findAllWithDetails(Pageable pageable);

    // Keep the old method for non-paginated use cases
    @Query("SELECT d FROM WhatsAppLogDetail d " +
           "JOIN FETCH d.whatsAppLog l " +
           "JOIN FETCH l.template " +
           "JOIN FETCH l.provider " +
           "ORDER BY d.attemptAt DESC")
    List<WhatsAppLogDetail> findAllByOrderByAttemptAtDesc();
}
