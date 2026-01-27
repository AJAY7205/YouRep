package com.learning.ytrep.service;

import com.learning.ytrep.exception.APIException;
import com.learning.ytrep.exception.ResourceNotFoundException;
import com.learning.ytrep.model.User;
import com.learning.ytrep.model.Video;
import com.learning.ytrep.model.VideoAnalytics;
//import com.learning.ytrep.model.VideoAnalytics;
import com.learning.ytrep.model.VideoStatus;
import com.learning.ytrep.payload.VideoAnalyticsResponse;
import com.learning.ytrep.payload.VideoDTO;
import com.learning.ytrep.payload.VideoResponse;
import com.learning.ytrep.payload.VideoUploadRequest;
import com.learning.ytrep.repository.UserRepository;
import com.learning.ytrep.repository.VideoRepository;

import org.modelmapper.ModelMapper;
// import org.modelmapper.ModelMapper;
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
    private final ModelMapper modelMapper;
    private final ThumbnailService thumbnailService;
    private final UserRepository userRepository;

    public VideoServiceImpl(VideoRepository videoRepository,StorageService storageService,VideoAnalyticsServiceImpl videoAnalyticsServiceImpl,ModelMapper modelMapper,ThumbnailService thumbnailService,UserRepository userRepository){
        this.videoRepository = videoRepository;
        this.storageService = storageService;
        this.videoAnalyticsServiceImpl = videoAnalyticsServiceImpl;
        this.modelMapper = modelMapper;
        this.thumbnailService = thumbnailService;
        this.userRepository = userRepository;
    }

    @Override
    public VideoDTO postVideo(VideoUploadRequest videoUploadRequest, MultipartFile file,MultipartFile thumbnail, String username){
        // Video video = modelMapper.map(videoDTO,Video.class);
//        video.setVideoId(videoDTO.getVideoId());
        User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        Video video = new Video();
        video.setVideoId(null);
        video.setTitle(videoUploadRequest.getTitle());
        video.setStatus(VideoStatus.UPLOADED);
        video.setDescription(videoUploadRequest.getDescription());
        video.setCreatedAt(LocalDateTime.now());
        video.setUpdatedAt(LocalDateTime.now());
        video.setUser(user);
        String object = storageService.uploadVideo(file);
        video.setObjectKey(object);
        if(thumbnail != null && !thumbnail.isEmpty()){
            String thumbnailKey = thumbnailService.uploadThumbnail(thumbnail);
            video.setThumbnailkey(thumbnailKey);
        }
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
        if (video.getThumbnailkey() != null) {
            dto.setThumbnailUrl("/videos/" + video.getVideoId() + "/thumbnail");
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

    @Override
    public VideoResponse getAllVideo(){
        List<Video> videos = videoRepository.findAll();
        // if(videos == null){
        //     throw new ResourceNotFoundException("No Videos Uploaded Found");
        // }
        List<VideoDTO> videoDTOs = videos.stream().map(video -> mapToDTO(video)).toList();
        VideoResponse videoResponse = new VideoResponse();
        videoResponse.setContent(videoDTOs);
        return videoResponse;
    }

    @Override
    public VideoResponse deleteVideo(Long videoId){
        Video video = videoRepository.findByVideoId(videoId);
        if(video == null){
            throw new ResourceNotFoundException("Video", "VideoID", videoId.toString());
        }
        storageService.deleteVideo(video.getObjectKey());
        if(video.getThumbnailkey() != null){
            thumbnailService.deleteThumbnailCache(videoId);
            storageService.deleteThumbnail(video.getThumbnailkey());
        }

        
        VideoDTO videoDTO = mapToDTO(video);
        VideoResponse videoResponse = new VideoResponse();
        // videoResponse.setContent(List.of(video));
        videoResponse.setContent(List.of(videoDTO));
        videoRepository.delete(video);
        return videoResponse;
    }
}
