package com.learning.ytrep.service;

// import com.learning.ytrep.config.MinIOConfig;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetObjectArgs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.learning.ytrep.exception.APIException;

import java.io.InputStream;
// import java.io.IOException;
import java.util.UUID;

@Service
public class StorageService {

    @Value("${minio.video}")
    private String VIDEO_BUCKET_NAME;

    @Value("${minio.thumbnail}")
    private String THUMBNAIL_BUCKET_NAME;

    private final MinioClient minIOConfig;

    public StorageService(MinioClient minIOConfig){
        this.minIOConfig = minIOConfig;
    }

    public String uploadVideo(MultipartFile file){
        try {
            String objectKey = "videos/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
            minIOConfig.putObject(
                    PutObjectArgs.builder()
                            .bucket(VIDEO_BUCKET_NAME)
                            .object(objectKey)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            return objectKey;
        } catch (Exception e) {
            throw new APIException("Notitu poyiruchu Thambi");
        }
        

    }
    public InputStream getVideoStream(String objectKey) {
            try {
                return minIOConfig.getObject(
                    GetObjectArgs.builder()
                        .bucket(VIDEO_BUCKET_NAME)
                        .object(objectKey)
                        .build()
                    );
            } catch (Exception e) {
            throw new RuntimeException("Failed to fetch video", e);
        }
    }
    public long getVideoSize(String objectKey) {
        try {
            return minIOConfig.statObject(
                io.minio.StatObjectArgs.builder()
                    .bucket(VIDEO_BUCKET_NAME)
                    .object(objectKey)
                    .build()
            ).size();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get video size", e);
        }
    }

    public String uploadThumbnail(MultipartFile file){
        try{
            String objectKey = "thumbnails/" +UUID.randomUUID() + "-" + file.getOriginalFilename();
            minIOConfig.putObject(
                PutObjectArgs.builder()
                .bucket(THUMBNAIL_BUCKET_NAME)
                .object(objectKey)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build()
            );
            return objectKey;
        }catch(Exception e){
            throw new RuntimeException("Failed to upload thumbnail", e);
        }
    }
    public byte[] getThumbnail(String objectKey){
        try{
            InputStream stream = minIOConfig.getObject(
                GetObjectArgs.builder()
                .bucket(THUMBNAIL_BUCKET_NAME)
                .object(objectKey)
                .build()
            );
            return stream.readAllBytes();
        }catch(Exception e){
            throw new RuntimeException("Failed to fetch thumbnail",e);
        }
    }

    public void deleteVideo(String objectKey){
        try{
            minIOConfig.removeObject(
                io.minio.RemoveObjectArgs.builder()
                .bucket(VIDEO_BUCKET_NAME)
                .object(objectKey)
                .build()
            );
        }catch(Exception e){
            throw new APIException("Failed to delete video");
        }
    }

    public void deleteThumbnail(String objectKey){
        try{
            minIOConfig.removeObject(
                io.minio.RemoveObjectArgs.builder()
                .bucket(THUMBNAIL_BUCKET_NAME)
                .object(objectKey)
                .build()
            );
        }catch(Exception e){
            throw new APIException("Failed to delete video");
        }
    }
}
