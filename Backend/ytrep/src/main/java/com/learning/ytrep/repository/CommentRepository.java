package com.learning.ytrep.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.learning.ytrep.model.Comment;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByVideoVideoIdAndParentIsNullOrderByCreatedAtDesc(Long videoId);
    List<Comment> findByVideoVideoIdOrderByCreatedAtDesc(Long videoId);
    List<Comment> findByParentCommentIdOrderByCreatedAtAsc(Long parentId);
    long countByVideoVideoId(Long videoId);
}
