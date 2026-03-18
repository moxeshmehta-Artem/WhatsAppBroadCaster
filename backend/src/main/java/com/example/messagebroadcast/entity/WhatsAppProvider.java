package com.example.messagebroadcast.entity;

import com.example.messagebroadcast.enums.ProviderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tbWhatsAppProvider")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsAppProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "providerID")
    private Long providerID;

    @Column(name = "providerName", nullable = false, unique = true)
    private String providerName; // e.g. "INFOBIP" or "360DIALOG"

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "varchar(255) default 'ACTIVE'")
    @Builder.Default
    private ProviderStatus status = ProviderStatus.ACTIVE;
}
