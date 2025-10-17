package com.postread.dto;

import com.postread.data.User;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArticleResponse {
    private Long id;
    private String title;
    private String shortDescription;
    private UserDTO author;
    private boolean isPublished;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int viewCount;
    private List<ArticleBlockDTO> blocks;
}