package com.learning.ytrep.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoAnalyticsDTO {
    private long videoId;
    private long viewCount;
    private long likeCount;
    private String createdAt;
    private String updatedAt;
}
