package com.example.messagebroadcast.entity;

import com.example.messagebroadcast.enums.MessageStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbWhatsAppLogDetails")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsAppLogDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "logDetailID")
    private Long logDetailID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "whatsAppLogID", nullable = false)
    private WhatsAppLog whatsAppLog;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private MessageStatus status; 

    @Column(name = "errorMessage", columnDefinition = "TEXT")
    private String errorMessage; // The raw error or response from the provider

    @CreationTimestamp
    @Column(name = "attemptAt", updatable = false)
    private LocalDateTime attemptAt;
}
