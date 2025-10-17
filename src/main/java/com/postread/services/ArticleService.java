package com.postread.services;

import com.postread.repositories.ArticleBlockRepository;
import com.postread.repositories.ArticleRepository;
import com.postread.dto.ArticleBlockDTO;
import com.postread.dto.ArticleResponse;
import com.postread.dto.UserDTO;
import com.postread.data.*;
import com.postread.repositories.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ArticleService {
    private final ArticleRepository articleRepository;
    private final ArticleBlockRepository blockRepository;
    private final UserRepository userRepository;

    // Создание статьи с блоками
    public Article createArticle(String title, String shortDescription, List<BlockRequest> blocks, Long authorId) {
        User author = userRepository.findById(authorId)
                .orElseGet(() -> {
                    User tempUser = new User();
                    tempUser.setId(authorId);
                    tempUser.setName("Автор " + authorId);
                    tempUser.setEmail("author" + authorId + "@example.com");
                    return userRepository.save(tempUser);
                });

        Article article = new Article();
        article.setAuthor(author);
        article.setTitle(title);
        article.setShortDescription(shortDescription);
        article.setPublished(false);

        Article savedArticle = articleRepository.save(article);

        if (blocks != null) {
            for (int i = 0; i < blocks.size(); i++) {
                BlockRequest blockRequest = blocks.get(i);

                ArticleBlock block = new ArticleBlock();
                block.setArticle(savedArticle);
                block.setType(blockRequest.getType());
                block.setContent(blockRequest.getContent());
                block.setOrder(blockRequest.getOrder() != null ? blockRequest.getOrder() : i);

                blockRepository.save(block);
            }
        }

        return savedArticle;
    }

    // Получение статьи с блоками как Entity
    public Article getArticleWithBlocks(Long id) {
        return articleRepository.findByIdWithBlocks(id)
                .orElseThrow(() -> new RuntimeException("Article not found"));
    }

    // Получение статьи как DTO (без циклической зависимости)
    public ArticleResponse getArticleResponse(Long id) {
        Article article = getArticleWithBlocks(id);
        return convertToResponse(article);
    }

    // Конвертация Article в ArticleResponse
    public ArticleResponse convertToResponse(Article article) {
        ArticleResponse response = new ArticleResponse();
        response.setId(article.getId());
        response.setTitle(article.getTitle());
        response.setShortDescription(article.getShortDescription());
        response.setPublished(article.isPublished());
        response.setCreatedAt(article.getCreatedAt());
        response.setUpdatedAt(article.getUpdatedAt());
        response.setViewCount(article.getViewCount());

        // Конвертируем автора
        if (article.getAuthor() != null) {
            UserDTO userDTO = new UserDTO();
            userDTO.setId(article.getAuthor().getId());
            userDTO.setUsername(article.getAuthor().getName());
            userDTO.setEmail(article.getAuthor().getEmail());
            response.setAuthor(userDTO);
        }

        // Конвертируем блоки
        if (article.getBlocks() != null) {
            List<ArticleBlockDTO> blockDTOs = article.getBlocks().stream()
                    .map(block -> {
                        ArticleBlockDTO blockDTO = new ArticleBlockDTO();
                        blockDTO.setId(block.getId());
                        blockDTO.setType(block.getType());
                        blockDTO.setContent(block.getContent());
                        blockDTO.setOrder(block.getOrder());
                        blockDTO.setCreatedAt(block.getCreatedAt());
                        return blockDTO;
                    })
                    .collect(Collectors.toList());
            response.setBlocks(blockDTOs);
        }

        return response;
    }

    // Поиск по названию
    public List<Article> searchByTitle(String title) {
        return articleRepository.findByTitleContainingIgnoreCase(title);
    }

    // Получение опубликованных статей
    public List<Article> getPublishedArticles() {
        return articleRepository.findByIsPublishedTrue();
    }

    // Получение всех статей
    public List<Article> getAllArticles() {
        return articleRepository.findAll();
    }

    // DTO для запроса - должен совпадать с тем, что используется в контроллере
    @Data
    public static class BlockRequest {
        private String type;
        private String content;
        private Integer order;
    }
}