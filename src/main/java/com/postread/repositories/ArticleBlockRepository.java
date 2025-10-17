package com.postread.repositories;

import com.postread.data.ArticleBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleBlockRepository extends JpaRepository<ArticleBlock, Long> {
    List<ArticleBlock> findByArticleIdOrderByOrderAsc(Long articleId);
}