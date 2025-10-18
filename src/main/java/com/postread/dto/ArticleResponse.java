package com.postread.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArticleResponse {
    private Long id;
    private String title;
    private String shortDescription;
    private UserDTODeprecated author;
    private boolean isPublished;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int viewCount;
    private List<ArticleBlockDTODeprecated> blocks;
}