package com.postread.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentDTO {
    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UserSimpleDTO author;
    private Long articleId;
    private Long parentCommentId; // для ответов на комментарии

    public CommentDTO() {}
}