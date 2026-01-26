package com.learning.ytrep.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.learning.ytrep.payload.VideoAnalyticsResponse;
import com.learning.ytrep.service.VideoAnalyticsService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api")
public class VideoAnalyticsController {
    
    private final VideoAnalyticsService videoAnalyticsService;

    public VideoAnalyticsController(VideoAnalyticsService videoAnalyticsService) {
        this.videoAnalyticsService = videoAnalyticsService;
    }

    @Operation(summary = "Getting Likes and Views of a Video")
    @GetMapping("/video-analytics/{videoId}")
    public ResponseEntity<VideoAnalyticsResponse> getVideoAnalytics(@PathVariable Long videoId){
        VideoAnalyticsResponse response = videoAnalyticsService.getVideoAnalytics(videoId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "Increasing the views")
    @PostMapping("/video-analyitcs/{videoId}/increment-views")
    public ResponseEntity<VideoAnalyticsResponse> updateViews(@PathVariable Long videoId){
        VideoAnalyticsResponse response = videoAnalyticsService.incrementViewCount(videoId);
        return new ResponseEntity<>(response,HttpStatus.ACCEPTED);
    }

    @Operation(summary = "Increasing the likes")
    @PostMapping("/video-analyitcs/{videoId}/increment-likes")
    public ResponseEntity<VideoAnalyticsResponse> incrementLikes(@PathVariable Long videoId){
        VideoAnalyticsResponse response = videoAnalyticsService.incrementLikeCount(videoId);
        return new ResponseEntity<>(response,HttpStatus.ACCEPTED);
    }

    @Operation(summary = "Decreasing the likes")
    @PostMapping("/video-analyitcs/{videoId}/decrement-likes")
    public ResponseEntity<VideoAnalyticsResponse> decrementLikes(@PathVariable Long videoId){
        VideoAnalyticsResponse response = videoAnalyticsService.decrementLikeCount(videoId);
        return new ResponseEntity<>(response,HttpStatus.ACCEPTED);
    }
}
