package com.postread.repositories;

import com.postread.data.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

        // Поиск по названию (оставляем как было)
        List<Article> findByTitleContainingIgnoreCase(String title);
        List<Article> findByIsPublishedTrue();

        // Новые методы для работы с блоками
        @Query("SELECT a FROM Article a LEFT JOIN FETCH a.blocks WHERE a.id = :id")
        Optional<Article> findByIdWithBlocks(@Param("id") Long id);

        List<Article> findByAuthorId(Long authorId);

        List<Article> findAll();
}