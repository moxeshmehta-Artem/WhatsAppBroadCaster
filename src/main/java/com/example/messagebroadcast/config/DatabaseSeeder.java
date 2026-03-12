package com.example.messagebroadcast.config;

import com.example.messagebroadcast.entity.WhatsAppTemp;
import com.example.messagebroadcast.repository.WhatsAppTemplateRepository;
import com.example.messagebroadcast.repository.WhatsAppProviderRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseSeeder {

    @Bean
    CommandLineRunner initDatabase(WhatsAppTemplateRepository templateRepo, WhatsAppProviderRepository providerRepo) {
        return args -> {
            // Seed Providers
            if (providerRepo.count() == 0) {
                providerRepo.save(com.example.messagebroadcast.entity.WhatsAppProvider.builder().providerName("INFOBIP").build());
                providerRepo.save(com.example.messagebroadcast.entity.WhatsAppProvider.builder().providerName("360DIALOG").build());
                System.out.println("✅ Seeded WhatsApp Providers!");
            }

            // Seed Templates
            if (templateRepo.count() == 0) {
                String templateContent = "Gently,\n" +
                                         "There is a medical camp of Dr.Vora,\n" +
                                         "it will arrange it on {{Date}} at {{address}}.\n" +
                                         "Thank You.";
                
                WhatsAppTemp template = WhatsAppTemp.builder()
                        .name("MedicalCamp")
                        .content(templateContent)
                        .build();
                
                templateRepo.save(template);
                System.out.println("=====================================================");
                System.out.println("✅ Automatically created 'MedicalCamp' template in DB!");
                System.out.println("Template ID: " + template.getId());
                System.out.println("=====================================================");
            }
        };
    }
}
