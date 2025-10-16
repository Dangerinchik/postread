package com.postread.data;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "multimedia", indexes = {
        @Index(name = "idx_multimedia_article", columnList = "article_id")
})
@AllArgsConstructor
@NoArgsConstructor
public class Multimedia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "article_id")
    private Article article;

    @Column(name = "url", length = 255)
    private String url;
}