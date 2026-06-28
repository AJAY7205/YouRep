package com.learning.ytrep.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.learning.ytrep.payload.APIResponse;
import com.learning.ytrep.payload.CommentDTO;
import com.learning.ytrep.payload.CommentRequest;
import com.learning.ytrep.payload.CommentResponse;
import com.learning.ytrep.service.CommentService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @Operation(summary = "Get comments for a video (public)")
    @GetMapping("/{videoId}")
    public ResponseEntity<CommentResponse> getComments(
            @PathVariable Long videoId,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        CommentResponse response = commentService.getComments(videoId, username);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "Get comment count for a video (public)")
    @GetMapping("/{videoId}/count")
    public ResponseEntity<Long> getCommentCount(@PathVariable Long videoId) {
        long count = commentService.getCommentCount(videoId);
        return new ResponseEntity<>(count, HttpStatus.OK);
    }

    @Operation(summary = "Create a comment on a video")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    @PostMapping("/{videoId}")
    public ResponseEntity<CommentDTO> createComment(
            @PathVariable Long videoId,
            @Valid @RequestBody CommentRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        CommentDTO comment = commentService.createComment(videoId, request, username);
        return new ResponseEntity<>(comment, HttpStatus.CREATED);
    }

    @Operation(summary = "Reply to a comment")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    @PostMapping("/{videoId}/reply/{parentId}")
    public ResponseEntity<CommentDTO> replyToComment(
            @PathVariable Long videoId,
            @PathVariable Long parentId,
            @Valid @RequestBody CommentRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        CommentDTO reply = commentService.addReply(videoId, parentId, request, username);
        return new ResponseEntity<>(reply, HttpStatus.CREATED);
    }

    @Operation(summary = "Toggle like on a comment")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    @PostMapping("/{commentId}/like")
    public ResponseEntity<APIResponse> toggleLike(
            @PathVariable Long commentId,
            Authentication authentication) {
        String username = authentication.getName();
        boolean liked = commentService.toggleLike(commentId, username);
        String message = liked ? "Comment liked" : "Comment unliked";
        return new ResponseEntity<>(new APIResponse(message, true), HttpStatus.OK);
    }

    @Operation(summary = "Delete a comment (owner only)")
    @PreAuthorize("hasAuthority('ADMIN') or @commentSecurityService.isCommentOwner(authentication, #commentId)")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<APIResponse> deleteComment(
            @PathVariable Long commentId,
            Authentication authentication) {
        String username = authentication.getName();
        commentService.deleteComment(commentId, username);
        return new ResponseEntity<>(new APIResponse("Comment deleted", true), HttpStatus.OK);
    }
}
