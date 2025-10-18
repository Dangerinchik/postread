package com.postread.services;

import com.postread.data.*;
import com.postread.dto.*;
import com.postread.repositories.ArticleRepository;
import com.postread.repositories.UserRepository;
import com.postread.security.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final TagService tagService;

    @Transactional
    public Article createArticle(String title, String shortDescription,
                                 List<ArticleBlock> blocks, Long authorId,
                                 boolean isPublished, Set<String> tagNames,
                                 Long reviewForArticleId) {

        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Article article = new Article();
        article.setAuthor(author);
        article.setTitle(title);
        article.setShortDescription(shortDescription);
        article.setPublished(isPublished);

        // Устанавливаем связь с оригинальной статьей, если это рецензия
        if (reviewForArticleId != null) {
            Article originalArticle = articleRepository.findById(reviewForArticleId)
                    .orElseThrow(() -> new RuntimeException("Оригинальная статья не найдена"));
            article.setReviewForArticle(originalArticle);
        }

        // Устанавливаем блоки
        if (blocks != null) {
            blocks.forEach(block -> block.setArticle(article));
            article.setBlocks(blocks);
        }

        // Обрабатываем теги
        if (tagNames != null && !tagNames.isEmpty()) {
            Set<Tag> tags = tagNames.stream()
                    .map(tagService::findOrCreateTag)
                    .collect(Collectors.toSet());
            article.setTags(tags);
        }

        return articleRepository.save(article);
    }

    // Метод для получения рецензий на статью
    public List<Article> getReviewsForArticle(Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Статья не найдена"));
        return articleRepository.findByReviewForArticleAndPublishedTrueOrderByCreatedAtDesc(article);
    }

    // Метод для проверки, написал ли пользователь уже рецензию на статью
    public boolean hasUserReviewedArticle(User user, Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Статья не найдена"));
        return articleRepository.existsByAuthorAndReviewForArticle(user, article);
    }

    // Безопасное получение количества рецензий
    public int getReviewsCount(Long articleId) {
        return articleRepository.countReviewsByArticleId(articleId);
    }

    // Безопасная проверка наличия рецензий
    public boolean hasReviews(Long articleId) {
        return articleRepository.existsReviewsByArticleId(articleId);
    }

    // Получение статьи по ID с инициализированными блоками
    public Article getArticleWithBlocks(Long articleId) {
        return articleRepository.findByIdWithBlocks(articleId)
                .orElseThrow(() -> new RuntimeException("Статья не найдена"));
    }

    // Получение статьи по ID с полной информацией (исправленная версия)
    public Article getArticleWithFullInfo(Long articleId) {
        try {
            // Сначала пытаемся загрузить с блоками
            Optional<Article> articleWithBlocks = articleRepository.findByIdWithAuthorAndTagsAndBlocks(articleId);
            if (articleWithBlocks.isPresent()) {
                return articleWithBlocks.get();
            }

            // Если не получилось, загружаем без блоков и затем блоки отдельно
            Article article = articleRepository.findByIdWithAuthorAndTags(articleId)
                    .orElseThrow(() -> new RuntimeException("Статья не найдена"));

            // Загружаем блоки отдельным запросом чтобы избежать дублирования
            List<ArticleBlock> blocks = articleRepository.findBlocksByArticleId(articleId);
            article.setBlocks(blocks);

            return article;
        } catch (Exception e) {
            // Fallback: загружаем базовую статью
            Article article = articleRepository.findById(articleId)
                    .orElseThrow(() -> new RuntimeException("Статья не найдена"));

            // Загружаем блоки отдельным запросом
            List<ArticleBlock> blocks = articleRepository.findBlocksByArticleId(articleId);
            article.setBlocks(blocks);

            return article;
        }
    }

    // Метод для увеличения счетчика просмотров
    @Transactional
    public void incrementViewCount(Long articleId) {
        articleRepository.incrementViewCount(articleId);
    }

    // Создание DTO для статьи с безопасным доступом к данным
    public ArticleDTO createArticleDTO(Article article) {
        ArticleDTO dto = new ArticleDTO();
        dto.setId(article.getId());
        dto.setTitle(article.getTitle());
        dto.setShortDescription(article.getShortDescription());
        dto.setPublished(article.isPublished());
        dto.setViewCount(article.getViewCount());
        dto.setCreatedAt(article.getCreatedAt());
        dto.setUpdatedAt(article.getUpdatedAt());
        dto.setReview(article.isReview());

        // Безопасная установка автора
        if (article.getAuthor() != null) {
            UserSimpleDTO authorDTO = new UserSimpleDTO();
            authorDTO.setId(article.getAuthor().getId());
            authorDTO.setName(article.getAuthor().getName());
            authorDTO.setIcon(article.getAuthor().getIcon());
            dto.setAuthor(authorDTO);
        }

        // Безопасная установка блоков
        if (article.getBlocks() != null && !article.getBlocks().isEmpty()) {
            List<ArticleBlockDTO> blockDTOs = article.getBlocks().stream()
                    .map(this::convertToBlockDTO)
                    .collect(Collectors.toList());
            dto.setBlocks(blockDTOs);
        } else {
            dto.setBlocks(List.of()); // Пустой список вместо null
        }

        // Безопасная установка тегов
        if (article.getTags() != null && !article.getTags().isEmpty()) {
            Set<TagDTO> tagDTOs = article.getTags().stream()
                    .map(this::convertToTagDTO)
                    .collect(Collectors.toSet());
            dto.setTags(tagDTOs);
        } else {
            dto.setTags(Set.of()); // Пустой набор вместо null
        }

        // Безопасная установка оригинальной статьи для рецензии
        if (article.isReview() && article.getReviewForArticle() != null) {
            ArticleSimpleDTO originalDTO = new ArticleSimpleDTO();
            originalDTO.setId(article.getReviewForArticle().getId());
            originalDTO.setTitle(article.getReviewForArticle().getTitle());
            originalDTO.setShortDescription(article.getReviewForArticle().getShortDescription());
            originalDTO.setPublished(article.getReviewForArticle().isPublished());

            if (article.getReviewForArticle().getAuthor() != null) {
                UserSimpleDTO originalAuthorDTO = new UserSimpleDTO();
                originalAuthorDTO.setId(article.getReviewForArticle().getAuthor().getId());
                originalAuthorDTO.setName(article.getReviewForArticle().getAuthor().getName());
                originalDTO.setAuthor(originalAuthorDTO);
            }

            dto.setReviewForArticle(originalDTO);
        }

        // Безопасное получение количества рецензий
        dto.setReviewsCount(getReviewsCount(article.getId()));
        dto.setHasReviews(hasReviews(article.getId()));

        return dto;
    }

    // Конвертер для блоков
    private ArticleBlockDTO convertToBlockDTO(ArticleBlock block) {
        ArticleBlockDTO dto = new ArticleBlockDTO();
        dto.setId(block.getId());
        dto.setType(block.getType());
        dto.setContent(block.getContent());
        dto.setOrder(block.getOrder());
        return dto;
    }

    // Конвертер для тегов
    private TagDTO convertToTagDTO(Tag tag) {
        TagDTO dto = new TagDTO();
        dto.setId(tag.getId());
        dto.setName(tag.getName());
        return dto;
    }

    // Получение статьи с безопасным DTO и увеличением счетчика просмотров
    @Transactional
    public ArticleDTO getArticleDTO(Long id) {
        try {
            // Сначала увеличиваем счетчик просмотров
            incrementViewCount(id);

            // Затем загружаем статью
            Article article = getArticleWithFullInfo(id);
            ArticleDTO dto = createArticleDTO(article);

            return dto;
        } catch (Exception e) {
            System.err.println("Ошибка при получении статьи ID " + id + ": " + e.getMessage());
            throw e;
        }
    }

    // Получение DTO без увеличения счетчика просмотров (для внутреннего использования)
    public ArticleDTO getArticleDTOWithoutIncrement(Long id) {
        Article article = getArticleWithFullInfo(id);
        return createArticleDTO(article);
    }

    // Получение статьи по ID
    public Article getArticle(Long articleId) {
        return articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Статья не найдена"));
    }

    // Методы поиска
    public List<Article> searchArticlesByTitle(String title) {
        return articleRepository.findByTitleContainingIgnoreCaseAndPublishedTrue(title);
    }

    public List<Article> searchArticlesByTags(List<String> tagNames) {
        return articleRepository.findByTagsNameInAndPublishedTrue(tagNames);
    }

    public List<Article> searchArticlesByTitleAndTags(String title, List<String> tags) {
        if (title != null && !title.trim().isEmpty() && tags != null && !tags.isEmpty()) {
            return articleRepository.findByTitleContainingIgnoreCaseAndTagsNameInAndPublishedTrue(title, tags);
        } else if (title != null && !title.trim().isEmpty()) {
            return searchArticlesByTitle(title);
        } else if (tags != null && !tags.isEmpty()) {
            return searchArticlesByTags(tags);
        } else {
            return articleRepository.findAllByPublishedTrueOrderByCreatedAtDesc();
        }
    }
}