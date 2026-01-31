package com.learning.ytrep.service;

import com.learning.ytrep.exception.APIException;
import com.learning.ytrep.util.ProgressTrackingInputStream;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetObjectArgs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
public class StorageService {

    @Value("${minio.video}")
    private String VIDEO_BUCKET_NAME;

    @Value("${minio.thumbnail}")
    private String THUMBNAIL_BUCKET_NAME;

    private final MinioClient minIOConfig;
    
    @Autowired(required = false)
    private UploadProgressTracker progressTracker;

    public StorageService(MinioClient minIOConfig) {
        this.minIOConfig = minIOConfig;
    }

    public String uploadVideoWithProgress(MultipartFile file, String uploadId) {
        String objectKey = "videos/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
        
        try {
            long fileSize = file.getSize();
            InputStream inputStream = file.getInputStream();
            
            ProgressTrackingInputStream progressStream = new ProgressTrackingInputStream(
                inputStream,
                fileSize,
                (bytesRead, totalBytes, percentage) -> {
                    
                    if (progressTracker != null) {
                        progressTracker.updateProgress(uploadId, bytesRead, totalBytes, percentage);
                    }
                }
            );
            
            minIOConfig.putObject(
                PutObjectArgs.builder()
                    .bucket(VIDEO_BUCKET_NAME)
                    .object(objectKey)
                    .stream(progressStream, fileSize, -1)
                    .contentType(file.getContentType())
                    .build()
            );
            
            if (progressTracker != null) {
                progressTracker.completeUpload(uploadId);
            }
            
            return objectKey;
            
        } catch (Exception e) {
            if (progressTracker != null) {
                progressTracker.failUpload(uploadId, e.getMessage());
            }
            throw new RuntimeException("Failed to upload video: " + e.getMessage(), e);
        }
    }

    // Original upload method (keep for backward compatibility)
    public String uploadVideo(MultipartFile file) {
        String objectKey = "videos/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
        
        try {
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
            throw new RuntimeException("Failed to upload video", e);
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

    public String uploadThumbnail(MultipartFile file) {
        try {
            String objectKey = "thumbnails/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
            minIOConfig.putObject(
                PutObjectArgs.builder()
                    .bucket(THUMBNAIL_BUCKET_NAME)
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );
            return objectKey;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload thumbnail", e);
        }
    }

    public byte[] getThumbnail(String objectKey) {
        try {
            InputStream stream = minIOConfig.getObject(
                GetObjectArgs.builder()
                    .bucket(THUMBNAIL_BUCKET_NAME)
                    .object(objectKey)
                    .build()
            );
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch thumbnail", e);
        }
    }

    public void deleteVideo(String objectKey) {
        try {
            minIOConfig.removeObject(
                io.minio.RemoveObjectArgs.builder()
                    .bucket(VIDEO_BUCKET_NAME)
                    .object(objectKey)
                    .build()
            );
        } catch (Exception e) {
            throw new APIException("Failed to delete video");
        }
    }

    public void deleteThumbnail(String objectKey) {
        try {
            minIOConfig.removeObject(
                io.minio.RemoveObjectArgs.builder()
                    .bucket(THUMBNAIL_BUCKET_NAME)
                    .object(objectKey)
                    .build()
            );
        } catch (Exception e) {
            throw new APIException("Failed to delete thumbnail");
        }
    }
}
