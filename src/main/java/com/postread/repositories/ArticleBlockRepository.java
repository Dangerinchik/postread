package com.postread.repositories;

import com.postread.data.ArticleBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleBlockRepository extends JpaRepository<ArticleBlock, Long> {
    List<ArticleBlock> findByArticleIdOrderByOrderAsc(Long articleId);

    @Modifying
    @Query("DELETE FROM ArticleBlock b WHERE b.article.id = :articleId")
    void deleteByArticleId(@Param("articleId") Long articleId);
}