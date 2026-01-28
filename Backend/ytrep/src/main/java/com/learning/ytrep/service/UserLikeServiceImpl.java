package com.learning.ytrep.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.learning.ytrep.exception.ResourceNotFoundException;
import com.learning.ytrep.model.User;
import com.learning.ytrep.model.UserLike;
import com.learning.ytrep.model.Video;
import com.learning.ytrep.payload.VideoDTO;
import com.learning.ytrep.repository.UserLikeRepository;
import com.learning.ytrep.repository.UserRepository;
// import com.learning.ytrep.repository.VideoAnalyticsRepository;
import com.learning.ytrep.repository.VideoRepository;
// import com.learning.ytrep.service.VideoAnalyticsService;

@Service
public class UserLikeServiceImpl implements UserLikeService {

    private final UserLikeRepository userLikeRepository;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final VideoAnalyticsService videoAnalyticsService;
    private final ModelMapper modelMapper;

    public UserLikeServiceImpl(
            UserLikeRepository userLikeRepository,
            UserRepository userRepository,
            VideoRepository videoRepository,
            VideoAnalyticsService videoAnalyticsService,
            ModelMapper modelMapper) {
        this.userLikeRepository = userLikeRepository;
        this.userRepository = userRepository;
        this.videoRepository = videoRepository;
        this.videoAnalyticsService = videoAnalyticsService;
        this.modelMapper = modelMapper;
    }

    @Override
    @Transactional
    public void toggleLike(Long videoId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video", "videoId", String.valueOf(videoId)));

        var existingLike = userLikeRepository.findByUserUserIdAndVideoVideoId(user.getUserId(), videoId);

        if (existingLike.isPresent()) {
            // Unlike
            userLikeRepository.delete(existingLike.get());
            videoAnalyticsService.decrementLikeCount(videoId);
        } else {
            // Like
            UserLike userLike = new UserLike();
            userLike.setUser(user);
            userLike.setVideo(video);
            userLike.setLikedAt(LocalDateTime.now());
            userLikeRepository.save(userLike);
            videoAnalyticsService.incrementLikeCount(videoId);
        }
    }

    @Override
    public boolean hasUserLiked(Long videoId, String username) {
        User user = userRepository.findByUsername(username)
                .orElse(null);

        if (user == null) {
            return false;
        }

        return userLikeRepository.existsByUserUserIdAndVideoVideoId(user.getUserId(), videoId);
    }

    @Override
    public List<VideoDTO> getUserLikedVideos(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        List<UserLike> likes = userLikeRepository.findByUserUserId(user.getUserId());

        return likes.stream()
                .map(like -> {
                    VideoDTO dto = mapToDTO(like.getVideo());
                    dto.setLiked(true);
                    return dto;
                })
                .collect(Collectors.toList());
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
    public long getLikeCount(Long videoId) {
        // return userLikeRepository.findAll().stream()
        //         .filter(like -> like.getVideo().getVideoId().equals(videoId))
        //         .count();
                return userLikeRepository.countByVideoVideoId(videoId);
    }
}