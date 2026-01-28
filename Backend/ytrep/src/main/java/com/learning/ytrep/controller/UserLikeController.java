package com.learning.ytrep.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.learning.ytrep.payload.APIResponse;
import com.learning.ytrep.payload.VideoDTO;
import com.learning.ytrep.service.UserLikeService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/likes")
public class UserLikeController {

    private final UserLikeService userLikeService;

    public UserLikeController(UserLikeService userLikeService) {
        this.userLikeService = userLikeService;
    }

    @Operation(summary = "Toggle like on a video (USER/ADMIN only)")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    @PostMapping("/{videoId}")
    public ResponseEntity<APIResponse> toggleLike(
            @PathVariable Long videoId,
            Authentication authentication) {
        
        String username = authentication.getName();
        userLikeService.toggleLike(videoId, username);
        
        boolean isLiked = userLikeService.hasUserLiked(videoId, username);
        String message = isLiked ? "Video liked successfully" : "Video unliked successfully";
        
        return new ResponseEntity<>(new APIResponse(message, true), HttpStatus.OK);
    }

    @Operation(summary = "Check if user has liked a video")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    @GetMapping("/{videoId}/check")
    public ResponseEntity<Boolean> hasLiked(
            @PathVariable Long videoId,
            Authentication authentication) {
        
        String username = authentication.getName();
        boolean hasLiked = userLikeService.hasUserLiked(videoId, username);
        
        return new ResponseEntity<>(hasLiked, HttpStatus.OK);
    }

    @Operation(summary = "Get user's liked videos")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    @GetMapping("/my-likes")
    public ResponseEntity<List<VideoDTO>> getMyLikedVideos(Authentication authentication) {
        String username = authentication.getName();
        List<VideoDTO> likedVideos = userLikeService.getUserLikedVideos(username);
        
        return new ResponseEntity<>(likedVideos, HttpStatus.OK);
    }

    @Operation(summary = "Get like count for a video")
    @GetMapping("/{videoId}/count")
    public ResponseEntity<Long> getLikeCount(@PathVariable Long videoId) {
        long count = userLikeService.getLikeCount(videoId);
        return new ResponseEntity<>(count, HttpStatus.OK);
    }
}