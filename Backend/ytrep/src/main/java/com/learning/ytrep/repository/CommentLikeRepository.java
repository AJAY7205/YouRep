package com.learning.ytrep.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.learning.ytrep.model.CommentLike;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    Optional<CommentLike> findByUserUserIdAndCommentCommentId(Long userId, Long commentId);
    boolean existsByUserUserIdAndCommentCommentId(Long userId, Long commentId);
    long countByCommentCommentId(Long commentId);
    void deleteByCommentCommentId(Long commentId);
    void deleteByUserUserId(Long userId);
}
