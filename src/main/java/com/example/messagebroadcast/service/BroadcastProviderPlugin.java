package com.example.messagebroadcast.service;

import com.example.messagebroadcast.dto.SendMessageResponseDTO;

public interface BroadcastProviderPlugin {
    
    /**
     * Sends a message via the specific provider.
     * @param mobileNumber the recipient's mobile number
     * @param messageContent the actual content of the message
     * @return response indicating if the message was sent successfully and any error details
     */
    SendMessageResponseDTO sendMessage(String mobileNumber, String messageContent);
    
    /**
     * @return The unique identifier of this provider (e.g. "360dialog", "infobip")
     */
    String getProviderName();
}
