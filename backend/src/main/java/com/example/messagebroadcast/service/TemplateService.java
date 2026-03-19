package com.example.messagebroadcast.service;

import com.example.messagebroadcast.entity.WhatsAppTemp;
import com.example.messagebroadcast.repository.WhatsAppTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final WhatsAppTemplateRepository templateRepository;

    public List<WhatsAppTemp> getAllTemplates() {
        return templateRepository.findAll();
    }

    public WhatsAppTemp createTemplate(WhatsAppTemp template) {
        // ENFORCE UNIQUENESS: Check if name already exists
        if (templateRepository.existsByName(template.getName())) {
            throw new IllegalArgumentException("Template with name '" + template.getName() + "' already exists!");
        }
        return templateRepository.save(template);
    }
    
    public void deleteTemplate(Long id) {
        templateRepository.deleteById(id);
    }
}
