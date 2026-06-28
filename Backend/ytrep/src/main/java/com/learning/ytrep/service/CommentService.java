package com.learning.ytrep.service;

import com.learning.ytrep.payload.CommentDTO;
import com.learning.ytrep.payload.CommentRequest;
import com.learning.ytrep.payload.CommentResponse;

public interface CommentService {
    CommentDTO createComment(Long videoId, CommentRequest request, String username);
    CommentDTO addReply(Long videoId, Long parentId, CommentRequest request, String username);
    void deleteComment(Long commentId, String username);
    CommentResponse getComments(Long videoId, String currentUsername);
    long getCommentCount(Long videoId);
    boolean toggleLike(Long commentId, String username);
}
