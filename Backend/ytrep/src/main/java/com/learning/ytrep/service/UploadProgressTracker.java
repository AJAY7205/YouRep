package com.learning.ytrep.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UploadProgressTracker {

    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, UploadProgress> progressMap = new ConcurrentHashMap<>();

    public UploadProgressTracker(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void updateProgress(String uploadId, long uploadedBytes, long totalBytes, int percentage) {
        UploadProgress progress = new UploadProgress(
            uploadId,
            uploadedBytes,
            totalBytes,
            percentage,
            "IN_PROGRESS"
        );
        
        progressMap.put(uploadId, progress);
        
        // Send real-time update via WebSocket
        messagingTemplate.convertAndSend(
            "/topic/upload-progress/" + uploadId,
            progress
        );
        
        System.out.println("Upload Progress: " + uploadId + " - " + percentage + "%");
    }

    public void completeUpload(String uploadId) {
        UploadProgress progress = progressMap.get(uploadId);
        if (progress != null) {
            progress.setStatus("COMPLETED");
            progress.setPercentage(100);
            
            messagingTemplate.convertAndSend(
                "/topic/upload-progress/" + uploadId,
                progress
            );
            
            System.out.println("Upload Complete: " + uploadId);
            
            // Remove after 30 seconds
            new Thread(() -> {
                try {
                    Thread.sleep(30000);
                    progressMap.remove(uploadId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    public void failUpload(String uploadId, String error) {
        UploadProgress progress = progressMap.get(uploadId);
        if (progress != null) {
            progress.setStatus("FAILED");
            progress.setError(error);
            
            messagingTemplate.convertAndSend(
                "/topic/upload-progress/" + uploadId,
                progress
            );
        }
    }

    public UploadProgress getProgress(String uploadId) {
        return progressMap.get(uploadId);
    }

    // Inner class for upload progress data
    public static class UploadProgress {
        private String uploadId;
        private long uploadedBytes;
        private long totalBytes;
        private int percentage;
        private String status;
        private String error;
        private long timestamp;

        public UploadProgress(String uploadId, long uploadedBytes, long totalBytes, 
                            int percentage, String status) {
            this.uploadId = uploadId;
            this.uploadedBytes = uploadedBytes;
            this.totalBytes = totalBytes;
            this.percentage = percentage;
            this.status = status;
            this.timestamp = System.currentTimeMillis();
        }

        // Getters and setters
        public String getUploadId() { return uploadId; }
        public void setUploadId(String uploadId) { this.uploadId = uploadId; }
        
        public long getUploadedBytes() { return uploadedBytes; }
        public void setUploadedBytes(long uploadedBytes) { this.uploadedBytes = uploadedBytes; }
        
        public long getTotalBytes() { return totalBytes; }
        public void setTotalBytes(long totalBytes) { this.totalBytes = totalBytes; }
        
        public int getPercentage() { return percentage; }
        public void setPercentage(int percentage) { this.percentage = percentage; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}