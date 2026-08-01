package com.learning.ytrep.security.services;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.learning.ytrep.model.Comment;
import com.learning.ytrep.repository.CommentRepository;

@Component("commentSecurityService")
@SuppressWarnings("null")
public class CommentSecurityService {

    private final CommentRepository commentRepository;

    public CommentSecurityService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    public boolean isCommentOwner(Authentication authentication, Long commentId) {
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null) {
            return false;
        }
        String username = authentication.getName();
        return comment.getUser().getUsername().equals(username);
    }
}
