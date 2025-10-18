package com.postread.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleSimpleDTO {
    private Long id;
    private String title;
    private String shortDescription;
    private UserSimpleDTO author;
    private boolean published;
    private Integer views;
    private LocalDateTime createdAt;

    // Добавьте геттер для viewCount для совместимости
    public int getViewCount() {
        return views != null ? views : 0;
    }

    public void setViewCount(int viewCount) {
        this.views = viewCount;
    }


}