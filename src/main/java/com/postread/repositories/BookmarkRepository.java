package com.postread.repositories;

import com.postread.data.Bookmark;
import com.postread.security.User;
import com.postread.data.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    // Существующие методы
    boolean existsByUserAndArticle(User user, Article article);
    Optional<Bookmark> findByUserAndArticle(User user, Article article);

    @Query("SELECT b FROM Bookmark b JOIN FETCH b.article WHERE b.user = :user ORDER BY b.addedAt DESC")
    List<Bookmark> findByUserWithArticles(@Param("user") User user);
    long countByUser(User user);
    void deleteByUserAndArticle(User user, Article article);

    // ДОБАВЬТЕ ЭТИ МЕТОДЫ:

    // Проверка существования закладки по ID пользователя и статьи
    @Query("SELECT COUNT(b) > 0 FROM Bookmark b WHERE b.user.id = :userId AND b.article.id = :articleId")
    boolean existsByUserAndArticle(@Param("userId") Long userId, @Param("articleId") Long articleId);

    // Удаление закладки по ID пользователя и статьи
    @Modifying
    @Query("DELETE FROM Bookmark b WHERE b.user.id = :userId AND b.article.id = :articleId")
    void deleteByUserAndArticle(@Param("userId") Long userId, @Param("articleId") Long articleId);

    void deleteByArticleId(Long articleId);
}