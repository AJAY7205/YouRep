package com.learning.ytrep.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.learning.ytrep.exception.APIException;
import com.learning.ytrep.exception.ResourceNotFoundException;
import com.learning.ytrep.model.Comment;
import com.learning.ytrep.model.CommentLike;
import com.learning.ytrep.model.User;
import com.learning.ytrep.model.Video;
import com.learning.ytrep.payload.CommentDTO;
import com.learning.ytrep.payload.CommentRequest;
import com.learning.ytrep.payload.CommentResponse;
import com.learning.ytrep.repository.CommentLikeRepository;
import com.learning.ytrep.repository.CommentRepository;
import com.learning.ytrep.repository.UserRepository;
import com.learning.ytrep.repository.VideoRepository;

@Service
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;

    public CommentServiceImpl(
            CommentRepository commentRepository,
            CommentLikeRepository commentLikeRepository,
            UserRepository userRepository,
            VideoRepository videoRepository) {
        this.commentRepository = commentRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.userRepository = userRepository;
        this.videoRepository = videoRepository;
    }

    @Override
    @Transactional
    public CommentDTO createComment(Long videoId, CommentRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video", "videoId", String.valueOf(videoId)));

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setUser(user);
        comment.setVideo(video);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());

        Comment saved = commentRepository.save(comment);
        return mapToDTO(saved, user.getUserId());
    }

    @Override
    @Transactional
    public CommentDTO addReply(Long videoId, Long parentId, CommentRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video", "videoId", String.valueOf(videoId)));

        Comment parent = commentRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "commentId", String.valueOf(parentId)));

        if (!parent.getVideo().getVideoId().equals(videoId)) {
            throw new APIException("Parent comment does not belong to this video");
        }

        Comment reply = new Comment();
        reply.setContent(request.getContent());
        reply.setUser(user);
        reply.setVideo(video);
        reply.setParent(parent);
        reply.setCreatedAt(LocalDateTime.now());
        reply.setUpdatedAt(LocalDateTime.now());

        Comment saved = commentRepository.save(reply);
        return mapToDTO(saved, user.getUserId());
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, String username) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "commentId", String.valueOf(commentId)));

        if (!comment.getUser().getUsername().equals(username)) {
            throw new APIException("You can only delete your own comments");
        }

        commentRepository.delete(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public CommentResponse getComments(Long videoId, String currentUsername) {
        final Long currentUserId;
        if (currentUsername != null) {
            User user = userRepository.findByUsername(currentUsername).orElse(null);
            currentUserId = (user != null) ? user.getUserId() : null;
        } else {
            currentUserId = null;
        }

        List<Comment> topLevel = commentRepository.findByVideoVideoIdAndParentIsNullOrderByCreatedAtDesc(videoId);
        long totalCount = commentRepository.countByVideoVideoId(videoId);

        List<CommentDTO> commentDTOs = topLevel.stream()
                .map(c -> mapToDTOWithReplies(c, currentUserId))
                .collect(Collectors.toList());

        return new CommentResponse(commentDTOs, totalCount);
    }

    @Override
    @Transactional(readOnly = true)
    public long getCommentCount(Long videoId) {
        return commentRepository.countByVideoVideoId(videoId);
    }

    @Override
    @Transactional
    public boolean toggleLike(Long commentId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "commentId", String.valueOf(commentId)));

        var existingLike = commentLikeRepository.findByUserUserIdAndCommentCommentId(
                user.getUserId(), commentId);

        if (existingLike.isPresent()) {
            commentLikeRepository.delete(existingLike.get());
            comment.setLikeCount(comment.getLikeCount() - 1);
            commentRepository.save(comment);
            return false;
        } else {
            CommentLike like = new CommentLike();
            like.setUser(user);
            like.setComment(comment);
            like.setCreatedAt(LocalDateTime.now());
            commentLikeRepository.save(like);
            comment.setLikeCount(comment.getLikeCount() + 1);
            commentRepository.save(comment);
            return true;
        }
    }

    private CommentDTO mapToDTOWithReplies(Comment comment, Long currentUserId) {
        CommentDTO dto = mapToDTO(comment, currentUserId);

        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            dto.setReplies(comment.getReplies().stream()
                    .map(r -> mapToDTO(r, currentUserId))
                    .collect(Collectors.toList()));
        } else {
            dto.setReplies(new ArrayList<>());
        }

        return dto;
    }

    private CommentDTO mapToDTO(Comment comment, Long currentUserId) {
        CommentDTO dto = new CommentDTO();
        dto.setCommentId(comment.getCommentId());
        dto.setContent(comment.getContent());
        dto.setUsername(comment.getUser().getUsername());
        dto.setUserId(comment.getUser().getUserId());
        dto.setLikeCount(comment.getLikeCount());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setUpdatedAt(comment.getUpdatedAt());
        dto.setOwner(currentUserId != null && currentUserId.equals(comment.getUser().getUserId()));

        if (currentUserId != null) {
            dto.setLikedByCurrentUser(
                    commentLikeRepository.existsByUserUserIdAndCommentCommentId(currentUserId, comment.getCommentId()));
        }

        return dto;
    }
}
