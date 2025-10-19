package com.postread.repositories;

import com.postread.data.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Найти все комментарии для статьи, отсортированные по дате (новые сверху)
    List<Comment> findByArticleIdOrderByCreatedAtDesc(Long articleId);

    // Найти комментарии для статьи с пагинацией
    @Query("SELECT c FROM Comment c WHERE c.article.id = :articleId ORDER BY c.createdAt DESC")
    List<Comment> findCommentsByArticleIdWithPagination(@Param("articleId") Long articleId);

    // Найти корневые комментарии (без родителя)
    List<Comment> findByArticleIdAndParentCommentIsNullOrderByCreatedAtDesc(Long articleId);

    // Найти ответы на конкретный комментарий
    List<Comment> findByParentCommentIdOrderByCreatedAtAsc(Long parentCommentId);

    // Количество комментариев для статьи
    Long countByArticleId(Long articleId);

    // Удалить все комментарии для статьи
    @Modifying
    @Transactional
    @Query("DELETE FROM Comment c WHERE c.article.id = :articleId")
    void deleteByArticleId(@Param("articleId") Long articleId);
}