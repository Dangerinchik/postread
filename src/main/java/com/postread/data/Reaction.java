package com.postread.data;

import com.postread.security.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "reactions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "article_id"})
        },
        indexes = {
                @Index(name = "idx_reactions_article", columnList = "article_id")
        })
@AllArgsConstructor
@NoArgsConstructor
public class Reaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "article_id")
    private Article article;

    @Column(name = "type")
    private Integer type; // 1-8 как в БД check constraint
}