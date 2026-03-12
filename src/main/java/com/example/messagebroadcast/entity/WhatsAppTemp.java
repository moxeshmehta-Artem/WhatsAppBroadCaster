package com.example.messagebroadcast.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tbWhatsAppTemp")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsAppTemp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

}
