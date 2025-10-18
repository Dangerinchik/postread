package com.postread.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
public class ArticleDTO {
    private Long id;
    private String title;
    private String shortDescription;
    private UserSimpleDTO author;
    private boolean published;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int viewCount;
    private List<ArticleBlockDTO> blocks;
    private Set<TagDTO> tags;

    // Исправленные поля для рецензий
    private boolean review; // измените с isReview на review
    private ArticleSimpleDTO reviewForArticle;
    private int reviewsCount;
    private boolean hasReviews;

    // Новые поля для комментариев
    private Long commentsCount;
    private boolean hasComments;

    public ArticleDTO() {}

    // Добавьте геттер для review (Thymeleaf использует геттеры)
    public boolean isReview() {
        return review;
    }

    public void setReview(boolean review) {
        this.review = review;
    }

    // Геттер для совместимости (если где-то используется isReview)
    public boolean getIsReview() {
        return review;
    }

    public void setIsReview(boolean review) {
        this.review = review;
    }

    public Long getCommentsCount() {
        return commentsCount != null ? commentsCount : 0L;
    }

    public boolean isHasComments() {
        return commentsCount != null && commentsCount > 0;
    }
}