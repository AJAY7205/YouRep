package com.learning.ytrep.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinIOConfig {

    @Value("minio.url")
    private String url;

    @Value("minio.access-key")
    private String username;

    @Value("minio.secret-key")
    private String password;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(url)
                .credentials(username,password)
                .build();
    }
}
