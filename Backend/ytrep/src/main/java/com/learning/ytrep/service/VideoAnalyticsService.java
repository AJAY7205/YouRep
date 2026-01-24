package com.learning.ytrep.service;

import com.learning.ytrep.payload.VideoAnalyticsResponse;

public interface VideoAnalyticsService {

    VideoAnalyticsResponse getVideoAnalytics(Long videoId);
    
    VideoAnalyticsResponse incrementViewCount(Long videoId);

    VideoAnalyticsResponse incrementLikeCount(Long videoId);

    VideoAnalyticsResponse decrementLikeCount(Long videoId);
}
