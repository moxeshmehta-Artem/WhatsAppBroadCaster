package com.example.messagebroadcast.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastRequestDTO {
    private String mobileNumber;
    private Long templateId;
    private String provider; // "360dialog" or "infobip"
    private Map<String, String> variables; // Mapping for {{varName}} in template content, optional
}
