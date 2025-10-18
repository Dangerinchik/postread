package com.postread.dto;

import lombok.Data;

@Data
public class ArticleBlockDTO {
    private Long id;
    private String type;
    private String content;
    private int order;
}