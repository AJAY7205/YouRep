package com.learning.ytrep.service;

import com.learning.ytrep.config.MinIOConfig;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class StorageService {

    @Value("${minio.bucket}")
    private static String BUCKET_NAME;

    private final MinioClient minIOConfig;

    public StorageService(MinioClient minIOConfig){
        this.minIOConfig = minIOConfig;
    }

    public String uploadVideo(MultipartFile file){
        try {
            String objectKey = UUID.randomUUID() + "-" + file.getOriginalFilename();
            minIOConfig.putObject(
                    PutObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(objectKey)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            return objectKey;
        } catch (Exception e) {
            throw new RuntimeException("Notitu poyiruchu Thambi",e);
        }
    }
}
