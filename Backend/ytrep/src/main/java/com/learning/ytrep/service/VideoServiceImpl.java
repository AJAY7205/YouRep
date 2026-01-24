package com.learning.ytrep.service;

import com.learning.ytrep.exception.APIException;
import com.learning.ytrep.exception.ResourceNotFoundException;
import com.learning.ytrep.model.Video;
import com.learning.ytrep.model.VideoAnalytics;
//import com.learning.ytrep.model.VideoAnalytics;
import com.learning.ytrep.model.VideoStatus;
import com.learning.ytrep.payload.VideoAnalyticsResponse;
import com.learning.ytrep.payload.VideoDTO;
import com.learning.ytrep.payload.VideoResponse;
import com.learning.ytrep.payload.VideoUploadRequest;
import com.learning.ytrep.repository.VideoRepository;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;


@Service
public class VideoServiceImpl implements VideoService{

    private final VideoRepository videoRepository;
    private final StorageService storageService;
    private final VideoAnalyticsServiceImpl videoAnalyticsServiceImpl;
    // private final ModelMapper modelMapper;

    public VideoServiceImpl(VideoRepository videoRepository,StorageService storageService,VideoAnalyticsServiceImpl videoAnalyticsServiceImpl){
        this.videoRepository = videoRepository;
        this.storageService = storageService;
        this.videoAnalyticsServiceImpl = videoAnalyticsServiceImpl;
        // this.modelMapper = modelMapper;
    }

    @Override
    public VideoDTO postVideo(VideoUploadRequest videoUploadRequest, MultipartFile file){
        // Video video = modelMapper.map(videoDTO,Video.class);
//        video.setVideoId(videoDTO.getVideoId());
        Video video = new Video();
        video.setVideoId(null);
        video.setTitle(videoUploadRequest.getTitle());
        video.setStatus(VideoStatus.UPLOADED);
        video.setDescription(videoUploadRequest.getDescription());
        video.setCreatedAt(LocalDateTime.now());
        video.setUpdatedAt(LocalDateTime.now());
        String object = storageService.uploadVideo(file);
        video.setObjectKey(object);
        VideoAnalytics videoAnalytics = new VideoAnalytics();
        videoAnalytics.setVideo(video);
        videoAnalytics.setViewCount(0);
        videoAnalytics.setLikeCount(0);
        videoAnalytics.setCreatedAt(LocalDateTime.now());
        videoAnalytics.setUpdatedAt(LocalDateTime.now());
        video.setVideoAnalytics(videoAnalytics);
        // videoAnalytics.setVideo(video);
        Video savedVideo = videoRepository.save(video);
        return mapToDTO(savedVideo);
    }

    @Override
    public VideoResponse getVideo(Long videoId){
        Video video = videoRepository.findByVideoId(videoId);
        if(video == null){
            throw new ResourceNotFoundException("Video","ID",videoId.toString());
        }
        // VideoDTO videoDTO = modelMapper.map(video,VideoDTO.class);
        VideoDTO videoDTO = mapToDTO(video);
        VideoResponse videoResponse = new VideoResponse();
        videoResponse.setContent(List.of(videoDTO));
        return videoResponse;
    }

    @Override
    public InputStream streamVideo(Long videoId){
        Video video = videoRepository.findByVideoId(videoId);

        // VideoAnalytics videoAnalytics = 
        if(video == null){
            throw new ResourceNotFoundException("Video","ID",videoId.toString());
        }
        String objectKey = video.getObjectKey();
        VideoAnalyticsResponse videoAnalyticsResponse = videoAnalyticsServiceImpl.incrementViewCount(videoId);
        return storageService.getVideoStream(objectKey);
    }

    @Override
    public long getVideoSize(Long videoId){
        Video video = videoRepository.findByVideoId(videoId);
        if(video == null){
            throw new APIException("Video Not Found");
        }
        return storageService.getVideoSize(video.getObjectKey());
    }
    
    private VideoDTO mapToDTO(Video video) {
        VideoDTO dto = new VideoDTO();
        dto.setVideoId(video.getVideoId());
        dto.setTitle(video.getTitle());
        dto.setDescription(video.getDescription());
        // dto.setObjectKey(video.getObjectKey());
        dto.setVideoStatus(video.getStatus());
        dto.setCreatedAt(video.getCreatedAt());
        dto.setUpdatedAt(video.getUpdatedAt());
        
        if (video.getVideoAnalytics() != null) {
            dto.setViewCount(video.getVideoAnalytics().getViewCount());
            dto.setLikeCount(video.getVideoAnalytics().getLikeCount());
        }
        
        return dto;
    }
    @Override
    public VideoResponse updateVideo(VideoUploadRequest videoUploadRequest,Long videoId){
        Video video = videoRepository.findByVideoId(videoId);
        if(video == null){
            throw new ResourceNotFoundException("Video","ID",videoId.toString());
        }
        video.setTitle(videoUploadRequest.getTitle());
        video.setDescription(videoUploadRequest.getDescription());
        video.setUpdatedAt(LocalDateTime.now());
        Video savedVideo = videoRepository.save(video);
        // VideoDTO videoDTO = modelMapper.map(video, VideoDTO.class);
        VideoDTO videoDTO = mapToDTO(savedVideo);
        VideoResponse videoResponse = new VideoResponse();
        videoResponse.setContent(List.of(videoDTO));
        return videoResponse;
    }
}
