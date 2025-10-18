package com.postread.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.postread.security.User;

@Data
@Entity
@Table(name = "articles")
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(name = "title", nullable = false, length = 70)
    private String title;

    @Column(name = "short_description", length = 100)
    private String shortDescription;

    @Column(name = "is_published")
    private boolean published = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "view_count")
    private int viewCount = 0;

    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("order ASC")
    @JsonIgnore
    private List<ArticleBlock> blocks = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "articles_tags",
            joinColumns = @JoinColumn(name = "article_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @JsonIgnore
    private Set<Tag> tags = new HashSet<>();

    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<Reaction> reactions = new HashSet<>();

    // Новое поле: если не null, то это рецензия на указанную статью
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "review_for_article_id")
    @JsonIgnoreProperties({"reviews", "blocks", "reactions", "tags"})
    private Article reviewForArticle;

    // Связь для получения всех рецензий на эту статью
    @OneToMany(mappedBy = "reviewForArticle", fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<Article> reviews = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean hasTags() {
        return tags != null && !tags.isEmpty();
    }

    // Вспомогательный метод для проверки, является ли статья рецензией
    public boolean isReview() {
        return reviewForArticle != null;
    }

    // Убраны проблемные методы getReviewsCount() и hasReviews()
    // Вместо них используем безопасные методы в сервисе
}