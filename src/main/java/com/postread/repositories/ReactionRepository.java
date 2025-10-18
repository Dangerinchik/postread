package com.postread.repositories;

import com.postread.data.Article;
import com.postread.data.Reaction;
import com.postread.security.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReactionRepository extends JpaRepository<Reaction, Long> {

    Optional<Reaction> findByUserAndArticle(User user, Article article);

    List<Reaction> findByArticleId(Long articleId);

    @Query("SELECT r.type, COUNT(r) FROM Reaction r WHERE r.article.id = :articleId GROUP BY r.type")
    List<Object[]> getReactionCountsByArticleId(@Param("articleId") Long articleId);

    boolean existsByUserAndArticle(User user, Article article);

    void deleteByUserAndArticle(User user, Article article);

    int countByArticleIdAndType(Long articleId, Integer type);
}