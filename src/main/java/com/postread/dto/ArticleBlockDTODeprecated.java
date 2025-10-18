package com.postread.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleBlockDTODeprecated {
    private Long id;
    private String type;
    private String content;
    private Integer order;
    private LocalDateTime createdAt;
}
