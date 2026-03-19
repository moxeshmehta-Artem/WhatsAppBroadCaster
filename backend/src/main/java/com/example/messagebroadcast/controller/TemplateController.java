package com.example.messagebroadcast.controller;

import com.example.messagebroadcast.entity.WhatsAppTemp;
import com.example.messagebroadcast.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @GetMapping
    public ResponseEntity<List<WhatsAppTemp>> getAllTemplates() {
        return ResponseEntity.ok(templateService.getAllTemplates());
    }

    @PostMapping
    public ResponseEntity<?> createTemplate(@RequestBody WhatsAppTemp template) {
        try {
            WhatsAppTemp created = templateService.createTemplate(template);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            // This catches the uniqueness check!
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error creating template: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTemplate(@PathVariable Long id) {
        try {
            templateService.deleteTemplate(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting template: " + e.getMessage());
        }
    }
}
