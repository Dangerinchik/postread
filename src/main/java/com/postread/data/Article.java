package com.postread.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.postread.security.User;

@Data
@Entity
@Table(name = "articles")
@AllArgsConstructor
@NoArgsConstructor
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

    // Добавляем связь с блоками
    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("order ASC")
    @JsonIgnore // Добавляем эту аннотацию чтобы избежать циклической зависимости
    private List<ArticleBlock> blocks = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}