package com.learning.ytrep.service;

import java.time.LocalDateTime;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.learning.ytrep.exception.APIException;
import com.learning.ytrep.exception.ResourceNotFoundException;
import com.learning.ytrep.model.VideoAnalytics;
import com.learning.ytrep.payload.VideoAnalyticsDTO;
import com.learning.ytrep.payload.VideoAnalyticsResponse;
import com.learning.ytrep.repository.VideoAnalyticsRepository;

import jakarta.transaction.Transactional;

@Service
public class VideoAnalyticsServiceImpl implements VideoAnalyticsService {

    private final VideoAnalyticsRepository videoAnalyticsRepository;
    private final ModelMapper modelMapper;

    public VideoAnalyticsServiceImpl(VideoAnalyticsRepository videoAnalyticsRepository,ModelMapper modelMapper) {
        this.videoAnalyticsRepository = videoAnalyticsRepository;
        this.modelMapper = modelMapper;
    }
    
    @Override
    public VideoAnalyticsResponse getVideoAnalytics(Long videoId) {
        VideoAnalytics videoAnalytics = videoAnalyticsRepository.findByVideoId(videoId);
        if(videoAnalytics == null){
            throw new ResourceNotFoundException("VideoAnalytics", "videoId", videoId.toString()); 
        }
        VideoAnalyticsDTO videoAnalyticsDTO = new VideoAnalyticsDTO();
        videoAnalyticsDTO = modelMapper.map(videoAnalytics,VideoAnalyticsDTO.class);
        VideoAnalyticsResponse videoAnalyticsResponse = new VideoAnalyticsResponse();
        videoAnalyticsResponse.setContent(List.of(videoAnalyticsDTO));
        return videoAnalyticsResponse;
    }

    @Override
    @Transactional
    public VideoAnalyticsResponse incrementViewCount(Long videoId){
        VideoAnalytics videoAnalytics = videoAnalyticsRepository.findByVideoId(videoId);
        if(videoAnalytics == null){
            throw new ResourceNotFoundException("VideoAnalytics","ID",videoId.toString());
        }
        videoAnalytics.setViewCount(videoAnalytics.getViewCount() + 1);
        videoAnalytics.setUpdatedAt(LocalDateTime.now());
        VideoAnalytics updatedAnalytics = videoAnalyticsRepository.save(videoAnalytics);
        VideoAnalyticsDTO videoAnalyticsDTO = modelMapper.map(updatedAnalytics, VideoAnalyticsDTO.class);
        VideoAnalyticsResponse videoAnalyticsResponse = new VideoAnalyticsResponse();
        videoAnalyticsResponse.setContent(List.of(videoAnalyticsDTO));
        return videoAnalyticsResponse;
    }
    @Override
    @Transactional
    public VideoAnalyticsResponse incrementLikeCount(Long videoId) {
        VideoAnalytics videoAnalytics = videoAnalyticsRepository.findByVideoId(videoId);
        if(videoAnalytics == null){
            throw new ResourceNotFoundException("VideoAnalytics", "videoId", videoId.toString()); 
        }
        videoAnalytics.setLikeCount(videoAnalytics.getLikeCount() + 1);
        videoAnalytics.setUpdatedAt(LocalDateTime.now());
        VideoAnalytics updatedAnalytics = videoAnalyticsRepository.save(videoAnalytics);
        
        VideoAnalyticsDTO videoAnalyticsDTO = modelMapper.map(updatedAnalytics, VideoAnalyticsDTO.class);
        VideoAnalyticsResponse response = new VideoAnalyticsResponse();
        response.setContent(List.of(videoAnalyticsDTO));
        return response;
    }

    @Override
    @Transactional
    public VideoAnalyticsResponse decrementLikeCount(Long videoId) {
        VideoAnalytics videoAnalytics = videoAnalyticsRepository.findByVideoId(videoId);
        if(videoAnalytics == null){
            throw new ResourceNotFoundException("VideoAnalytics", "videoId", videoId.toString()); 
        }
        if(videoAnalytics.getLikeCount() > 0) {
            videoAnalytics.setLikeCount(videoAnalytics.getLikeCount() - 1);
            videoAnalytics.setUpdatedAt(LocalDateTime.now());
        }
        VideoAnalytics updated = videoAnalyticsRepository.save(videoAnalytics);
        
        VideoAnalyticsDTO videoAnalyticsDTO = modelMapper.map(updated, VideoAnalyticsDTO.class);
        VideoAnalyticsResponse videoAnalyticsResponse = new VideoAnalyticsResponse();
        videoAnalyticsResponse.setContent(List.of(videoAnalyticsDTO));
        return videoAnalyticsResponse;
    }
}
