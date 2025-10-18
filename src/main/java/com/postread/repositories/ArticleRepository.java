package com.postread.repositories;

import com.postread.data.Article;
import com.postread.data.ArticleBlock;
import com.postread.security.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

        // Существующие методы
        List<Article> findAllByPublishedTrueOrderByCreatedAtDesc();
        List<Article> findByTitleContainingIgnoreCaseAndPublishedTrue(String title);

        // Методы для поиска по тегам
        @Query("SELECT DISTINCT a FROM Article a JOIN a.tags t WHERE t.name IN :tagNames AND a.published = true")
        List<Article> findByTagsNameInAndPublishedTrue(@Param("tagNames") List<String> tagNames);

        @Query("SELECT DISTINCT a FROM Article a JOIN a.tags t WHERE a.title LIKE %:title% AND t.name IN :tagNames AND a.published = true")
        List<Article> findByTitleContainingIgnoreCaseAndTagsNameInAndPublishedTrue(
                @Param("title") String title,
                @Param("tagNames") List<String> tagNames);

        // Новые методы для работы с рецензиями
        List<Article> findByReviewForArticleAndPublishedTrueOrderByCreatedAtDesc(Article article);
        boolean existsByAuthorAndReviewForArticle(User author, Article reviewForArticle);

        // Метод для поиска статей (не рецензий)
        @Query("SELECT a FROM Article a WHERE a.reviewForArticle IS NULL AND a.published = true ORDER BY a.createdAt DESC")
        List<Article> findAllOriginalArticles();

        // Метод для поиска рецензий пользователя
        List<Article> findByAuthorAndReviewForArticleIsNotNullOrderByCreatedAtDesc(User author);

        // Стандартный метод поиска по ID
        Optional<Article> findById(Long id);

        List<Article> findByAuthorId(Long id);

        // Безопасные методы для работы с рецензиями
        @Query("SELECT COUNT(r) FROM Article r WHERE r.reviewForArticle.id = :articleId AND r.published = true")
        int countReviewsByArticleId(@Param("articleId") Long articleId);

        @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Article r WHERE r.reviewForArticle.id = :articleId AND r.published = true")
        boolean existsReviewsByArticleId(@Param("articleId") Long articleId);

        // Исправленные методы для загрузки статьи с инициализированными блоками
        @Query("SELECT DISTINCT a FROM Article a LEFT JOIN FETCH a.blocks b WHERE a.id = :id ORDER BY b.order ASC")
        Optional<Article> findByIdWithBlocks(@Param("id") Long id);

        // Исправленный метод для загрузки статьи с инициализированными данными
        @Query("SELECT DISTINCT a FROM Article a LEFT JOIN FETCH a.author LEFT JOIN FETCH a.tags LEFT JOIN FETCH a.blocks b WHERE a.id = :id ORDER BY b.order ASC")
        Optional<Article> findByIdWithAuthorAndTagsAndBlocks(@Param("id") Long id);

        // Метод для загрузки без блоков (исправленное имя)
        @Query("SELECT DISTINCT a FROM Article a LEFT JOIN FETCH a.author LEFT JOIN FETCH a.tags WHERE a.id = :id")
        Optional<Article> findByIdWithAuthorAndTags(@Param("id") Long id);

        // Метод для загрузки блоков отдельным запросом
        @Query("SELECT b FROM ArticleBlock b WHERE b.article.id = :articleId ORDER BY b.order ASC")
        List<ArticleBlock> findBlocksByArticleId(@Param("articleId") Long articleId);

        // Метод для увеличения счетчика просмотров
        @Modifying
        @Transactional
        @Query("UPDATE Article a SET a.viewCount = a.viewCount + 1 WHERE a.id = :articleId")
        void incrementViewCount(@Param("articleId") Long articleId);

        // Дополнительный метод для получения статьи только с автором
        @Query("SELECT a FROM Article a LEFT JOIN FETCH a.author WHERE a.id = :id")
        Optional<Article> findByIdWithAuthor(@Param("id") Long id);
}