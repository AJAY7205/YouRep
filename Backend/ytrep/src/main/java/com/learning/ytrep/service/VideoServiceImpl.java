package com.learning.ytrep.service;

import com.learning.ytrep.exception.APIException;
import com.learning.ytrep.exception.ResourceNotFoundException;
import com.learning.ytrep.model.Comment;
import com.learning.ytrep.model.User;
import com.learning.ytrep.model.UserLike;
import com.learning.ytrep.model.Video;
import com.learning.ytrep.model.VideoAnalytics;
import com.learning.ytrep.model.VideoStatus;
import com.learning.ytrep.payload.TranscodeRequestDTO;
import com.learning.ytrep.payload.VideoAnalyticsResponse;
import com.learning.ytrep.payload.VideoDTO;
import com.learning.ytrep.payload.VideoResponse;
import com.learning.ytrep.payload.VideoUploadRequest;
import com.learning.ytrep.repository.CommentLikeRepository;
import com.learning.ytrep.repository.CommentRepository;
import com.learning.ytrep.repository.UserLikeRepository;
import com.learning.ytrep.repository.UserRepository;
import com.learning.ytrep.repository.VideoRepository;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Service
public class VideoServiceImpl implements VideoService{

    private static final Logger log = LoggerFactory.getLogger(VideoServiceImpl.class);

    private final VideoRepository videoRepository;
    private final StorageService storageService;
    private final VideoAnalyticsServiceImpl videoAnalyticsServiceImpl;
    @SuppressWarnings("unused")
    private final ModelMapper modelMapper;
    private final ThumbnailService thumbnailService;
    private final UserRepository userRepository;
    private final UserLikeRepository userLikeRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final TranscodeRequestProducer transcodeRequestProducer;

    public VideoServiceImpl(VideoRepository videoRepository, StorageService storageService, VideoAnalyticsServiceImpl videoAnalyticsServiceImpl, ModelMapper modelMapper, ThumbnailService thumbnailService, UserRepository userRepository, UserLikeRepository userLikeRepository, CommentRepository commentRepository, CommentLikeRepository commentLikeRepository, TranscodeRequestProducer transcodeRequestProducer){
        this.videoRepository = videoRepository;
        this.storageService = storageService;
        this.videoAnalyticsServiceImpl = videoAnalyticsServiceImpl;
        this.modelMapper = modelMapper;
        this.thumbnailService = thumbnailService;
        this.userRepository = userRepository;
        this.userLikeRepository = userLikeRepository;
        this.commentRepository = commentRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.transcodeRequestProducer = transcodeRequestProducer;
    }

    @Override
    public VideoDTO postVideo(VideoUploadRequest videoUploadRequest, MultipartFile file,MultipartFile thumbnail, String username){
        User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Video video = new Video();
        video.setVideoId(null);
        video.setTitle(videoUploadRequest.getTitle());
        video.setStatus(VideoStatus.UPLOADING);
        video.setDescription(videoUploadRequest.getDescription());
        video.setCreatedAt(LocalDateTime.now());
        video.setUpdatedAt(LocalDateTime.now());
        video.setUser(user);

        String objectKey = storageService.uploadVideo(file);
        video.setObjectKey(objectKey);
        video.setStatus(VideoStatus.UPLOADED);

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
        Video savedVideo = videoRepository.save(video);

        // Queue for transcoding
        savedVideo.setStatus(VideoStatus.PROCESSING);
        savedVideo.setUpdatedAt(LocalDateTime.now());
        videoRepository.save(savedVideo);
        transcodeRequestProducer.sendTranscodeRequest(
                new TranscodeRequestDTO(savedVideo.getVideoId(), savedVideo.getObjectKey()));

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
        String objectKey = resolveStreamKey(video);
        @SuppressWarnings("unused")
        VideoAnalyticsResponse videoAnalyticsResponse = videoAnalyticsServiceImpl.incrementViewCount(videoId);
        return storageService.getVideoStream(objectKey);
    }

    @Override
    public InputStream streamVideoRange(Long videoId, long offset, long length){
        Video video = videoRepository.findByVideoId(videoId);
        if(video == null){
            throw new ResourceNotFoundException("Video","ID",videoId.toString());
        }
        if (offset == 0) {
            @SuppressWarnings("unused")
            VideoAnalyticsResponse videoAnalyticsResponse = videoAnalyticsServiceImpl.incrementViewCount(videoId);
        }
        return storageService.getVideoStreamRange(resolveStreamKey(video), offset, length);
    }

    @Override
    public VideoStreamInfo getVideoStreamInfo(Long videoId, long start, long requestedEnd) {
        Video video = videoRepository.findByVideoId(videoId);
        if (video == null) {
            throw new ResourceNotFoundException("Video", "ID", videoId.toString());
        }
        String objectKey = resolveStreamKey(video);
        long totalSize = storageService.getVideoSize(objectKey);
        if (start == 0) {
            @SuppressWarnings("unused")
            VideoAnalyticsResponse videoAnalyticsResponse = videoAnalyticsServiceImpl.incrementViewCount(videoId);
        }
        long end = (requestedEnd <= 0 || requestedEnd >= totalSize) ? totalSize - 1 : requestedEnd;
        long contentLength = end - start + 1;
        InputStream stream = storageService.getVideoStreamRange(objectKey, start, contentLength);
        return new VideoStreamInfo(stream, totalSize, start, end, contentLength);
    }

    @Override
    public long getVideoSize(Long videoId){
        Video video = videoRepository.findByVideoId(videoId);
        if(video == null){
            throw new APIException("Video Not Found");
        }
        return storageService.getVideoSize(resolveStreamKey(video));
    }

    private String resolveStreamKey(Video video) {
        if (video.getTranscodedKey() != null && !video.getTranscodedKey().isBlank()) {
            return video.getTranscodedKey();
        }
        return video.getObjectKey();
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
        if (video.getUser() != null) {
        dto.setUsername(video.getUser().getUsername());
        }   
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
    @Transactional
    public VideoResponse deleteVideo(Long videoId){
        Video video = videoRepository.findByVideoId(videoId);
        if(video == null){
            throw new ResourceNotFoundException("Video", "VideoID", videoId.toString());
        }
        List<UserLike> likes = userLikeRepository.findByVideoVideoId(videoId);
            if (!likes.isEmpty()) {
                userLikeRepository.deleteAll(likes);
                }
        // comment_likes are not cascaded by the entity graph, and comments are
        // not related to Video, so remove both before the video delete.
        List<Comment> topLevelComments = commentRepository
                .findByVideoVideoIdAndParentIsNullOrderByCreatedAtDesc(videoId);
        for (Comment comment : topLevelComments) {
            commentLikeRepository.deleteByCommentCommentId(comment.getCommentId());
            if (comment.getReplies() != null) {
                for (Comment reply : comment.getReplies()) {
                    commentLikeRepository.deleteByCommentCommentId(reply.getCommentId());
                }
            }
            commentRepository.delete(comment);
        }
        storageService.deleteVideo(video.getObjectKey());
        if(video.getTranscodedKey() != null){
            storageService.deleteVideo(video.getTranscodedKey());
        }
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
