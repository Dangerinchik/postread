package com.postread.dto;

import lombok.Data;

@Data
public class ArticleBlockRequest {
    private String type;
    private String content;
    private Integer order;
}