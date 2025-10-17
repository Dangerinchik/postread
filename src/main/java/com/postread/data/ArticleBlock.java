package com.postread.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "article_blocks")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArticleBlock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    @JsonIgnore // Добавляем эту аннотацию чтобы избежать циклической зависимости
    private Article article;

    @Column(name = "block_type", nullable = false)
    private String type; // "text", "media", "code"

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "block_order", nullable = false)
    private Integer order;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}