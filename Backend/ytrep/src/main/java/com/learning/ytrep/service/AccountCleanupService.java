package com.learning.ytrep.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.learning.ytrep.model.Comment;
import com.learning.ytrep.model.User;
import com.learning.ytrep.model.Video;
import com.learning.ytrep.repository.CommentLikeRepository;
import com.learning.ytrep.repository.CommentRepository;
import com.learning.ytrep.repository.UserLikeRepository;
import com.learning.ytrep.repository.UserRepository;
import com.learning.ytrep.repository.VideoRepository;

@Service
@EnableScheduling
public class AccountCleanupService {

    private static final Logger log = LoggerFactory.getLogger(AccountCleanupService.class);

    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final UserLikeRepository userLikeRepository;
    private final VideoService videoService;

    @Value("${app.verification.expiry-days:3}")
    private long expiryDays;

    public AccountCleanupService(UserRepository userRepository, VideoRepository videoRepository,
                                 CommentRepository commentRepository, CommentLikeRepository commentLikeRepository,
                                 UserLikeRepository userLikeRepository, VideoService videoService) {
        this.userRepository = userRepository;
        this.videoRepository = videoRepository;
        this.commentRepository = commentRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.userLikeRepository = userLikeRepository;
        this.videoService = videoService;
    }

    @Scheduled(cron = "0 30 4 * * *")
    @Transactional
    public void deleteUnverifiedAccounts() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(expiryDays);
        List<User> staleUsers = userRepository.findByEmailVerifiedFalseAndCreatedAtBefore(cutoff);

        if (staleUsers.isEmpty()) {
            return;
        }

        log.info("Deleting {} unverified account(s) older than {} day(s)", staleUsers.size(), expiryDays);

        for (User user : staleUsers) {
            try {
                deleteUserData(user);
            } catch (Exception e) {
                log.error("Failed to clean up account for {}", user.getEmail(), e);
            }
        }
    }

    private void deleteUserData(User user) {
        Long userId = user.getUserId();

        List<Video> videos = videoRepository.findByUserUserId(userId);
        for (Video video : videos) {
            videoService.deleteVideo(video.getVideoId());
        }

        List<Comment> comments = commentRepository.findByUserUserId(userId);
        for (Comment comment : comments) {
            commentLikeRepository.deleteByCommentCommentId(comment.getCommentId());
            if (comment.getReplies() != null) {
                for (Comment reply : comment.getReplies()) {
                    commentLikeRepository.deleteByCommentCommentId(reply.getCommentId());
                }
            }
            commentRepository.delete(comment);
        }

        commentLikeRepository.deleteByUserUserId(userId);
        userLikeRepository.deleteAll(userLikeRepository.findByUserUserId(userId));

        userRepository.delete(user);
        log.info("Deleted unverified account {}", user.getEmail());
    }
}
