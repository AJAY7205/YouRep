package com.learning.ytrep.payload;

import com.learning.ytrep.model.VideoStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoDTO {
    private Long videoId;
    private String title;
    private String description;
//    private String videoUrl;
    private VideoStatus videoStatus;
    private long viewCount;
    private long likeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String thumbnailUrl;
}
