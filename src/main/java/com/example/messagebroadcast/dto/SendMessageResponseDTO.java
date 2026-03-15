package com.example.messagebroadcast.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageResponseDTO {
    private boolean success;
    private String status;
    private String messageId;
    private String errorDetails;
}
