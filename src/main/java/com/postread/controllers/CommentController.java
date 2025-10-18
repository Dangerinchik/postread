package com.postread.controllers;

import com.postread.dto.CommentDTO;
import com.postread.repositories.UserRepository;
import com.postread.security.User;
import com.postread.services.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    private final UserRepository userRepository;

    // Получить текущего пользователя
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser")) {
            String username = authentication.getName();
            return userRepository.findByName(username)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + username));
        }
        throw new RuntimeException("Пользователь не аутентифицирован");
    }

    // Получить все комментарии для статьи
    @GetMapping("/article/{articleId}")
    public ResponseEntity<?> getCommentsForArticle(@PathVariable Long articleId) {
        try {
            List<CommentDTO> comments = commentService.getCommentsForArticle(articleId);
            Long commentsCount = commentService.getCommentsCountForArticle(articleId);

            Map<String, Object> response = new HashMap<>();
            response.put("comments", comments);
            response.put("totalCount", commentsCount);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка при загрузке комментариев: " + e.getMessage());
        }
    }

    // Создать новый комментарий
    @PostMapping("/article/{articleId}")
    public ResponseEntity<?> createComment(
            @PathVariable Long articleId,
            @RequestParam String content,
            @RequestParam(required = false) Long parentCommentId) {

        try {
            User currentUser = getCurrentUser();

            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Текст комментария не может быть пустым");
            }

            CommentDTO comment = commentService.createComment(content.trim(), articleId, currentUser.getId(), parentCommentId);
            return ResponseEntity.ok(comment);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Ошибка аутентификации: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка при создании комментария: " + e.getMessage());
        }
    }

    // Удалить комментарий
    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId) {
        try {
            User currentUser = getCurrentUser();
            commentService.deleteComment(commentId, currentUser.getId());

            return ResponseEntity.ok().body("Комментарий удален");

        } catch (RuntimeException e) {
            if (e.getMessage().contains("не найден")) {
                return ResponseEntity.notFound().build();
            } else if (e.getMessage().contains("только свои")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Ошибка аутентификации");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка при удалении комментария: " + e.getMessage());
        }
    }
}