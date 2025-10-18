package com.postread.repositories;

import com.postread.data.Article;
import com.postread.data.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

        List<Article> findAllByPublishedTrueOrderByCreatedAtDesc();

        List<Article> findByTitleContainingIgnoreCaseAndPublishedTrue(String title);

        // Поиск по тегам (хотя бы один тег совпадает) - ИСПРАВЛЕННЫЙ ЗАПРОС
        @Query("SELECT DISTINCT a FROM Article a " +
                "JOIN a.tags t " +
                "WHERE a.published = true AND t.name IN :tagNames")
        List<Article> findByTagsAndPublishedTrue(@Param("tagNames") List<String> tagNames);

        // Поиск по названию и тегам - ИСПРАВЛЕННЫЙ ЗАПРОС
        @Query("SELECT DISTINCT a FROM Article a " +
                "JOIN a.tags t " +
                "WHERE a.published = true " +
                "AND (LOWER(a.title) LIKE LOWER(CONCAT('%', :title, '%')) " +
                "AND t.name IN :tagNames)")
        List<Article> findByTitleContainingAndTagsAndPublishedTrue(
                @Param("title") String title,
                @Param("tagNames") List<String> tagNames);

        List<Article> findByAuthorId(Long authorId);

        @Query("SELECT a FROM Article a WHERE a.published = true AND a.author.id = :authorId")
        List<Article> findPublishedByAuthorId(@Param("authorId") Long authorId);

        @Query("SELECT a FROM Article a WHERE a.published = false AND a.author.id = :authorId")
        List<Article> findDraftsByAuthorId(@Param("authorId") Long authorId);

        // Новые методы для расширенного поиска
        @Query("SELECT a FROM Article a " +
                "WHERE LOWER(a.title) LIKE LOWER(CONCAT('%', :title, '%')) " +
                "AND a.published = :published " +
                "ORDER BY a.createdAt DESC")
        List<Article> findByTitleContainingIgnoreCaseAndPublished(@Param("title") String title,
                                                                  @Param("published") Boolean published);

        @Query("SELECT DISTINCT a FROM Article a " +
                "JOIN a.tags t " +
                "WHERE t IN :tags " +
                "AND a.published = :published " +
                "ORDER BY a.createdAt DESC")
        List<Article> findByTagsInAndPublished(@Param("tags") Set<Tag> tags,
                                               @Param("published") Boolean published);

        @Query("SELECT DISTINCT a FROM Article a " +
                "JOIN a.tags t " +
                "WHERE LOWER(a.title) LIKE LOWER(CONCAT('%', :title, '%')) " +
                "AND t IN :tags " +
                "AND a.published = :published " +
                "ORDER BY a.createdAt DESC")
        List<Article> findByTitleContainingAndTagsInAndPublished(@Param("title") String title,
                                                                 @Param("tags") Set<Tag> tags,
                                                                 @Param("published") Boolean published);

        @Query("SELECT a FROM Article a " +
                "JOIN a.author author " +
                "WHERE LOWER(author.name) LIKE LOWER(CONCAT('%', :authorName, '%')) " +
                "AND a.published = :published " +
                "ORDER BY a.createdAt DESC")
        List<Article> findByAuthorNameContainingIgnoreCaseAndPublished(@Param("authorName") String authorName,
                                                                       @Param("published") Boolean published);

        @Query("SELECT a FROM Article a WHERE a.published = :published ORDER BY a.createdAt DESC")
        List<Article> findByPublished(@Param("published") Boolean published);
}