package com.postread.dto;

import lombok.Data;

@Data
public class ArticleDto {
    private String title;
    private String shortDescription;
    private String text;
    private Long authorId;
    private boolean isPublished = false;
}
