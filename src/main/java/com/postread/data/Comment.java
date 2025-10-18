package com.postread.data;

import com.postread.security.User;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Data
@Entity
@Table(name = "comments", indexes = {
        @Index(name = "idx_comments_article", columnList = "article_id")
})
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "article_id")
    private Article article;

    @Column(name = "comment_text", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name = "reference_id")
    private Comment parentComment;

    @PrePersist
    protected void onCreate() {
//        ZoneId moscowZone = ZoneId.of("Europe/Moscow");
//        ZonedDateTime moscowTime = ZonedDateTime.now(moscowZone);
//        this.createdAt = moscowTime.toLocalDateTime();
//        this.updatedAt = moscowTime.toLocalDateTime();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
//        ZoneId moscowZone = ZoneId.of("Europe/Moscow");
//        ZonedDateTime moscowTime = ZonedDateTime.now(moscowZone);
        updatedAt = LocalDateTime.now();
    }
}