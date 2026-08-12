package com.learning.ytrep.service;

import com.learning.ytrep.exception.APIException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    @Value("${minio.video}")
    private String VIDEO_BUCKET_NAME;

    @Value("${minio.thumbnail}")
    private String THUMBNAIL_BUCKET_NAME;

    private final MinioClient minIOConfig;

    public StorageService(MinioClient minIOConfig) {
        this.minIOConfig = minIOConfig;
    }

    @PostConstruct
    public void initBuckets() {
        initBucket(VIDEO_BUCKET_NAME);
        initBucket(THUMBNAIL_BUCKET_NAME);
    }

    private void initBucket(String bucketName) {
        try {
            boolean exists = minIOConfig.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minIOConfig.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Created MinIO bucket: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Failed to initialize MinIO bucket '{}': {}", bucketName, e.getMessage());
        }
    }

    public String uploadVideoStream(InputStream inputStream, long fileSize, String fileName) {
        String objectKey = "videos/" + UUID.randomUUID() + "-" + fileName;
        try {
            minIOConfig.putObject(
                PutObjectArgs.builder()
                    .bucket(VIDEO_BUCKET_NAME)
                    .object(objectKey)
                    .stream(inputStream, fileSize, -1)
                    .contentType("video/mp4")
                    .build()
            );
            return objectKey;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload video stream", e);
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

    public InputStream getVideoStreamRange(String objectKey, long offset, long length) {
        try {
            return minIOConfig.getObject(
                GetObjectArgs.builder()
                    .bucket(VIDEO_BUCKET_NAME)
                    .object(objectKey)
                    .offset(offset)
                    .length(length)
                    .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch video range", e);
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
