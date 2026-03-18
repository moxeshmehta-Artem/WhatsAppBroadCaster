package com.example.messagebroadcast.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.util.List;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbWhatsAppLog")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsAppLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "whatsAppLogID")
    private Long whatsAppLogID;

    @Column(name = "mobileNo", nullable = false)
    private String mobileNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private WhatsAppTemp template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private WhatsAppProvider provider;

    @Column(name = "status")
    private String status; // e.g., "SENT", "FAILED"
    
    @JsonProperty("externalMessageId")
    @Column(name = "external_message_id")
    private String externalMessageId;

    // @OneToMany(mappedBy = "whatsAppLog", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    // private List<WhatsAppLogDetail> details;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
