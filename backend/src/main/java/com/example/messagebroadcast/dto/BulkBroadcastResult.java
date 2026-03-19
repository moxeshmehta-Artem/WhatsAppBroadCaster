package com.example.messagebroadcast.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BulkBroadcastResult {
    private String status;
    private int totalSuccessful;
    private int totalFailed;
    private String message;
}
