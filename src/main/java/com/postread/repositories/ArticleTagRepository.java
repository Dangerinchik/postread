package com.postread.repositories;

import com.postread.data.ArticleTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleTagRepository extends JpaRepository<ArticleTag, Long> {
    List<ArticleTag> findByArticleId(Long articleId);

    @Query("SELECT at FROM ArticleTag at JOIN at.article a WHERE a.published = true AND at.tag.id IN :tagIds")
    List<ArticleTag> findByTagIdsAndArticlePublished(@Param("tagIds") List<Long> tagIds);

    void deleteByArticleId(Long articleId);

    @Query("SELECT at FROM ArticleTag at JOIN at.article a WHERE a.published = true AND at.tag.name IN :tagNames")
    List<ArticleTag> findByTagNamesAndArticlePublished(@Param("tagNames") List<String> tagNames);
}
