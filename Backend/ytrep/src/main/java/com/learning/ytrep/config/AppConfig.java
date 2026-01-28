package com.learning.ytrep.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AppConfig {
    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")  // Changed from "/**"
                        .allowedOrigins(
                            "http://127.0.0.1:5500",  // Removed trailing slash
                            "http://localhost:5500"   // Added localhost variant
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("Content-Length", "Content-Range", "Accept-Ranges", "Authorization")
                        .allowCredentials(true)
                        .maxAge(3600);  // Cache preflight for 1 hour
            }
        };
    }
}
