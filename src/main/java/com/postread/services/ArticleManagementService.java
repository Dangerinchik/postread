package com.postread.services;

import com.postread.data.Article;
import com.postread.data.ArticleBlock;
import com.postread.data.Tag;
import com.postread.dto.ArticleDTO;
import com.postread.repositories.*;
import com.postread.security.User;
import com.postread.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleManagementService {

    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final TagService tagService;
    private final ArticleService articleService;
    private final CommentRepository commentRepository;
    private final ReactionRepository reactionRepository;
    private final BookmarkRepository bookmarkRepository;
    private final ArticleTagRepository articleTagRepository;

    /**
     * Получение черновиков пользователя
     */
    public List<Article> getUserDrafts(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        return articleRepository.findByAuthorAndPublishedFalseOrderByCreatedAtDesc(user);
    }

    /**
     * Получение опубликованных статей пользователя
     */
    public List<Article> getUserPublishedArticles(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        return articleRepository.findByAuthorAndPublishedTrueOrderByCreatedAtDesc(user);
    }

    /**
     * Проверка, принадлежит ли статья пользователю
     */
    public boolean isArticleOwner(Long articleId, Long userId) {
        return articleRepository.existsByIdAndAuthorId(articleId, userId);
    }

    /**
     * Получение статьи для редактирования (только если пользователь - владелец)
     */
    public Article getArticleForEditing(Long articleId, Long userId) {
        if (!isArticleOwner(articleId, userId)) {
            throw new RuntimeException("У вас нет прав для редактирования этой статьи");
        }

        return articleRepository.findByIdWithAuthorAndTagsAndBlocks(articleId)
                .orElseThrow(() -> new RuntimeException("Статья не найдена"));
    }

    /**
     * Получение DTO статьи для редактирования
     */
    public ArticleDTO getArticleDTOForEditing(Long articleId, Long userId) {
        Article article = getArticleForEditing(articleId, userId);
        return createEditArticleDTO(article);
    }

    /**
     * Обновление статьи
     */
    @Transactional
    public Article updateArticle(Long articleId, Long userId, String title,
                                 String shortDescription, List<ArticleBlock> newBlocks,
                                 boolean isPublished, Set<String> tagNames) {

        // Получаем статью без блоков, чтобы избежать проблем с orphanRemoval
        Article article = articleRepository.findByIdWithAuthorAndTags(articleId)
                .orElseThrow(() -> new RuntimeException("Статья не найдена"));

        if (!isArticleOwner(articleId, userId)) {
            throw new RuntimeException("У вас нет прав для редактирования этой статьи");
        }

        // Обновляем основные поля
        article.setTitle(title);
        article.setShortDescription(shortDescription);
        article.setPublished(isPublished);

        // УДАЛЯЕМ ВСЕ СУЩЕСТВУЮЩИЕ БЛОКИ ОТДЕЛЬНЫМ ЗАПРОСОМ
        articleRepository.deleteArticleBlocksByArticleId(articleId);

        // Принудительно синхронизируем с БД
        articleRepository.flush();

        // Теперь добавляем новые блоки в ОЧИЩЕННУЮ коллекцию
        if (newBlocks != null && !newBlocks.isEmpty()) {
            List<ArticleBlock> blocksToAdd = new ArrayList<>();
            for (int i = 0; i < newBlocks.size(); i++) {
                ArticleBlock block = new ArticleBlock();
                block.setType(newBlocks.get(i).getType());
                block.setContent(newBlocks.get(i).getContent());
                block.setOrder(i);
                block.setArticle(article);
                blocksToAdd.add(block);
            }
            // Используем существующую коллекцию, не создаем новую
            article.getBlocks().addAll(blocksToAdd);
        }

        // Обновляем теги
        if (tagNames != null && !tagNames.isEmpty()) {
            Set<Tag> tags = tagNames.stream()
                    .map(tagService::findOrCreateTag)
                    .collect(Collectors.toSet());
            article.setTags(tags);
        } else {
            article.getTags().clear();
        }

        return articleRepository.save(article);
    }

    /**
     * Простой и надежный метод через отдельные транзакции
     */
    @Transactional
    public Article updateArticleSimple(Long articleId, Long userId, String title,
                                       String shortDescription, List<ArticleBlock> newBlocks,
                                       boolean isPublished, Set<String> tagNames) {

        // Проверяем права
        if (!isArticleOwner(articleId, userId)) {
            throw new RuntimeException("У вас нет прав для редактирования этой статьи");
        }

        // Шаг 1: Удаляем все блоки в отдельной операции
        articleRepository.deleteArticleBlocksByArticleId(articleId);

        // Шаг 2: Обновляем статью без блоков
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Статья не найдена"));

        article.setTitle(title);
        article.setShortDescription(shortDescription);
        article.setPublished(isPublished);

        // Шаг 3: Добавляем новые блоки как новые сущности
        if (newBlocks != null && !newBlocks.isEmpty()) {
            for (int i = 0; i < newBlocks.size(); i++) {
                ArticleBlock newBlock = new ArticleBlock();
                newBlock.setType(newBlocks.get(i).getType());
                newBlock.setContent(newBlocks.get(i).getContent());
                newBlock.setOrder(i);
                newBlock.setArticle(article);
                article.getBlocks().add(newBlock);
            }
        }

        // Обновляем теги
        if (tagNames != null && !tagNames.isEmpty()) {
            Set<Tag> tags = tagNames.stream()
                    .map(tagService::findOrCreateTag)
                    .collect(Collectors.toSet());
            article.setTags(tags);
        }

        return articleRepository.save(article);
    }

    /**
     * Удаление статьи со всеми связанными сущностями
     */
    @Transactional
    public void deleteArticle(Long articleId, Long userId) {
        if (!isArticleOwner(articleId, userId)) {
            throw new RuntimeException("У вас нет прав для удаления этой статьи");
        }

        // Удаляем все связанные сущности в правильном порядке
        // 1. Комментарии
        commentRepository.deleteByArticleId(articleId);

        // 2. Реакции (лайки)
        reactionRepository.deleteByArticleId(articleId);

        // 3. Закладки
        bookmarkRepository.deleteByArticleId(articleId);

        // 4. Блоки статьи
        articleRepository.deleteArticleBlocksByArticleId(articleId);

        // 5. Связи с тегами
        articleTagRepository.deleteByArticleId(articleId);

        // 6. Рецензии (если эта статья является оригиналом для рецензий)
        deleteArticleReviews(articleId);

        // 7. Саму статью
        articleRepository.deleteById(articleId);
    }

    /**
     * Удаление рецензий на статью
     */
    private void deleteArticleReviews(Long articleId) {
        // Находим все рецензии на эту статью
        List<Article> reviews = articleRepository.findByReviewForArticleId(articleId);

        // Рекурсивно удаляем каждую рецензию со всеми её связями
        for (Article review : reviews) {
            deleteArticle(review.getId(), review.getAuthor().getId());
        }
    }

    /**
     * Публикация статьи
     */
    @Transactional
    public Article publishArticle(Long articleId, Long userId) {
        Article article = getArticleForEditing(articleId, userId);
        article.setPublished(true);
        return articleRepository.save(article);
    }

    /**
     * Перевод статьи в черновики
     */
    @Transactional
    public Article unpublishArticle(Long articleId, Long userId) {
        Article article = getArticleForEditing(articleId, userId);
        article.setPublished(false);
        return articleRepository.save(article);
    }

    /**
     * Создание DTO для редактирования
     */
    public ArticleDTO createEditArticleDTO(Article article) {
        return articleService.createArticleDTO(article);
    }
}