package com.example.messagebroadcast.service;

import com.example.messagebroadcast.dto.SendMessageResponseDTO;

public interface BroadcastProviderPlugin {

    SendMessageResponseDTO sendMessage(String mobileNumber, String messageContent);
    
    /**
     * @return The unique identifier of this provider (e.g. "360dialog", "infobip")
     */
    String getProviderName();
}
