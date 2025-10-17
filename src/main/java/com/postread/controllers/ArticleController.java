package com.postread.controllers;

import com.postread.dto.ArticleResponse;
import com.postread.data.Article;
import com.postread.services.ArticleService;
import com.postread.services.FileStorageService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {
    private final ArticleService articleService;
    private final FileStorageService fileStorageService;

    // Создание статьи с поддержкой загрузки файлов
    @PostMapping
    public ResponseEntity<?> createArticle(
            @RequestParam String title,
            @RequestParam String shortDescription,
            @RequestParam(required = false) Boolean isPublished,
            @RequestParam("blocks") String blocksJson) {

        try {
            Long temporaryAuthorId = 2L;

            // Парсим JSON строку с блоками
            List<BlockRequest> blocks = parseBlocksFromJson(blocksJson);

            // Конвертируем BlockRequest в ArticleService.BlockRequest
            List<ArticleService.BlockRequest> serviceBlocks = new ArrayList<>();

            if (blocks != null) {
                for (BlockRequest block : blocks) {
                    ArticleService.BlockRequest serviceBlock = new ArticleService.BlockRequest();
                    serviceBlock.setType(block.getType());
                    serviceBlock.setOrder(block.getOrder());

                    // Для медиа-блоков используем content (файлы уже обработаны на клиенте)
                    serviceBlock.setContent(block.getContent());

                    serviceBlocks.add(serviceBlock);
                }
            }

            Article article = articleService.createArticle(
                    title,
                    shortDescription,
                    serviceBlocks,
                    temporaryAuthorId
            );
            return ResponseEntity.ok(articleService.convertToResponse(article));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка при создании статьи: " + e.getMessage());
        }
    }

    // Вспомогательный метод для парсинга JSON
    private List<BlockRequest> parseBlocksFromJson(String blocksJson) {
        try {
            // Используем Jackson для парсинга JSON
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(blocksJson,
                    mapper.getTypeFactory().constructCollectionType(List.class, BlockRequest.class));
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при парсинге блоков: " + e.getMessage(), e);
        }
    }

    // Остальные методы остаются без изменений...
    // Получение статьи с блоками как DTO
    @GetMapping("/{id}")
    public ResponseEntity<?> getArticle(@PathVariable Long id) {
        try {
            ArticleResponse article = articleService.getArticleResponse(id);
            return ResponseEntity.ok(article);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Статья не найдена");
        }
    }

    // Поиск по названию
    @GetMapping("/search")
    public ResponseEntity<List<ArticleResponse>> searchArticles(@RequestParam String title) {
        List<Article> articles = articleService.searchByTitle(title);
        List<ArticleResponse> responses = articles.stream()
                .map(articleService::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    // Получение опубликованных статей
    @GetMapping("/published")
    public ResponseEntity<List<ArticleResponse>> getPublishedArticles() {
        List<Article> articles = articleService.getPublishedArticles();
        List<ArticleResponse> responses = articles.stream()
                .map(articleService::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    // Получение всех статей
    @GetMapping
    public ResponseEntity<List<ArticleResponse>> getAllArticles() {
        List<Article> articles = articleService.getAllArticles();
        List<ArticleResponse> responses = articles.stream()
                .map(articleService::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    // DTO для запроса (теперь без MultipartFile)
    @Data
    public static class BlockRequest {
        private String type;
        private String content;
        private Integer order;
    }
}