package com.learning.ytrep.payload;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO {
    private Long commentId;
    private String content;
    private String username;
    private Long userId;
    private long likeCount;
    private boolean likedByCurrentUser;
    private boolean owner;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CommentDTO> replies;
}
