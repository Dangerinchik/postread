package com.postread.dto;

import lombok.Data;

import java.util.List;

@Data
public class CreateArticleRequest {
    private String title;
    private String shortDescription;
    private boolean isPublished;
    private List<ArticleBlockRequest> blocks; // Блоки вместо простого текста
}