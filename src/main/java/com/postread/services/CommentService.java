package com.postread.services;

import com.postread.data.Comment;
import com.postread.data.Article;
import com.postread.security.User;
import com.postread.dto.CommentDTO;
import com.postread.dto.UserSimpleDTO;
import com.postread.repositories.CommentRepository;
import com.postread.repositories.ArticleRepository;
import com.postread.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    // Получить все комментарии для статьи
    public List<CommentDTO> getCommentsForArticle(Long articleId) {
        List<Comment> comments = commentRepository.findByArticleIdOrderByCreatedAtDesc(articleId);
        return comments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Получить количество комментариев для статьи
    public Long getCommentsCountForArticle(Long articleId) {
        return commentRepository.countByArticleId(articleId);
    }

    // Создать новый комментарий
    @Transactional
    public CommentDTO createComment(String content, Long articleId, Long userId, Long parentCommentId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Статья не найдена"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Comment comment = new Comment();
        comment.setContent(content);
        comment.setArticle(article);
        comment.setUser(user);

        // Если это ответ на другой комментарий
        if (parentCommentId != null) {
            Comment parentComment = commentRepository.findById(parentCommentId)
                    .orElseThrow(() -> new RuntimeException("Родительский комментарий не найден"));
            comment.setParentComment(parentComment);
        }

        Comment savedComment = commentRepository.save(comment);
        return convertToDTO(savedComment);
    }

    // Удалить комментарий
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Комментарий не найден"));

        // Проверяем, что пользователь является автором комментария
        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("Вы можете удалять только свои комментарии");
        }

        commentRepository.delete(comment);
    }

    // Конвертировать сущность в DTO
    private CommentDTO convertToDTO(Comment comment) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setUpdatedAt(comment.getUpdatedAt());
        dto.setArticleId(comment.getArticle().getId());

        if (comment.getParentComment() != null) {
            dto.setParentCommentId(comment.getParentComment().getId());
        }

        // Информация об авторе
        UserSimpleDTO authorDTO = new UserSimpleDTO();
        authorDTO.setId(comment.getUser().getId());
        authorDTO.setName(comment.getUser().getName());
        authorDTO.setIcon(comment.getUser().getIcon());
        dto.setAuthor(authorDTO);

        return dto;
    }
}