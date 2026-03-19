package com.example.messagebroadcast.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.example.messagebroadcast.enums.MessageStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageResponseDTO {
    private boolean success;
    private MessageStatus status;
    private String messageId;
    private String errorDetails;
}
