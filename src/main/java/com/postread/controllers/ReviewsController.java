package com.postread.controllers;

import com.postread.data.Article;
import com.postread.dto.ArticleSimpleDTO;
import com.postread.dto.UserSimpleDTO;
import com.postread.services.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ReviewsController {

    private final ArticleService articleService;

    @GetMapping("/{articleId}/blocks")
    public ResponseEntity<?> getArticleBlocks(@PathVariable Long articleId) {
        try {
            Article article = articleService.getArticleWithBlocks(articleId);
            if (article.getBlocks() != null) {
                return ResponseEntity.ok(article.getBlocks());
            }
            return ResponseEntity.ok(List.of());
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{articleId}/reviews")
    public ResponseEntity<?> getArticleReviews(@PathVariable Long articleId) {
        try {
            List<Article> reviews = articleService.getReviewsForArticle(articleId);
            List<ArticleSimpleDTO> reviewDTOs = reviews.stream()
                    .map(this::convertToSimpleDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(reviewDTOs);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(List.of());
        }
    }

    private ArticleSimpleDTO convertToSimpleDTO(Article article) {
        ArticleSimpleDTO dto = new ArticleSimpleDTO();
        dto.setId(article.getId());
        dto.setTitle(article.getTitle());
        dto.setShortDescription(article.getShortDescription());
        dto.setPublished(article.isPublished());
        dto.setViewCount(article.getViewCount());

        // Добавляем информацию об авторе
        if (article.getAuthor() != null) {
            UserSimpleDTO authorDTO = new UserSimpleDTO();
            authorDTO.setId(article.getAuthor().getId());
            authorDTO.setName(article.getAuthor().getName());
            authorDTO.setIcon(article.getAuthor().getIcon());
            dto.setAuthor(authorDTO);
        }

        return dto;
    }
}