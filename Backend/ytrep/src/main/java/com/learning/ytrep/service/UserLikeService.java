package com.learning.ytrep.service;

import java.util.List;

import com.learning.ytrep.payload.VideoDTO;

public interface UserLikeService {
    void toggleLike(Long videoId, String username);
    boolean hasUserLiked(Long videoId, String username);
    List<VideoDTO> getUserLikedVideos(String username);
    long getLikeCount(Long videoId);
}
