package com.example.messagebroadcast.controller;

import com.example.messagebroadcast.dto.BroadcastRequestDTO;
import com.example.messagebroadcast.entity.WhatsAppLog;
import com.example.messagebroadcast.service.BroadcastService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/broadcast")
@RequiredArgsConstructor
public class BroadcastController {

    private final BroadcastService broadcastService;

    @PostMapping
    public ResponseEntity<?> broadcastMessage(@RequestBody BroadcastRequestDTO requestDTO) {
        try {
            WhatsAppLog logResult = broadcastService.processAndBroadcast(requestDTO);
            return ResponseEntity.ok(logResult);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
