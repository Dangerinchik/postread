package com.postread.data;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Entity
@Table(name = "articles_tags", indexes = {
        @Index(name = "idx_articles_tags_tag", columnList = "tag_id")
})
@AllArgsConstructor
@NoArgsConstructor
public class ArticleTag {
    @EmbeddedId
    private ArticleTagKey id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("articleId")
    @JoinColumn(name = "article_id")
    private Article article;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("tagId")
    @JoinColumn(name = "tag_id")
    private Tag tag;

    public ArticleTag(Article article, Tag tag) {
        this.article = article;
        this.tag = tag;
        this.id = new ArticleTagKey(article.getId(), tag.getId());
    }
}
