package com.learning.ytrep.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TranscodeRequestDTO {

    private Long videoId;
    private String objectKey;
    private String bucketName;

    public TranscodeRequestDTO(Long videoId, String objectKey) {
        this.videoId = videoId;
        this.objectKey = objectKey;
        this.bucketName = "videos";
    }
}
