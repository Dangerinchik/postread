package com.postread.services;

import com.postread.data.Article;
import com.postread.data.Tag;
import com.postread.repositories.ArticleRepository;
import com.postread.repositories.TagRepository;
import com.postread.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ArticleService {

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private TagService tagService;

    /**
     * Поиск статей по названию и тегам
     */
    @Transactional(readOnly = true)
    public List<Article> searchArticlesByTitleAndTags(String title, List<String> tagNames) {
        if (tagNames != null && !tagNames.isEmpty()) {
            if (title != null && !title.trim().isEmpty()) {
                // Поиск по названию и тегам
                return articleRepository.findByTitleContainingAndTagsAndPublishedTrue(title.trim(), tagNames);
            } else {
                // Поиск только по тегам
                return articleRepository.findByTagsAndPublishedTrue(tagNames);
            }
        } else {
            // Поиск только по названию
            if (title != null && !title.trim().isEmpty()) {
                return articleRepository.findByTitleContainingIgnoreCaseAndPublishedTrue(title.trim());
            } else {
                // Если нет параметров, возвращаем все опубликованные статьи
                return articleRepository.findAllByPublishedTrueOrderByCreatedAtDesc();
            }
        }
    }

    /**
     * Поиск статей только по тегам
     */
    @Transactional(readOnly = true)
    public List<Article> searchArticlesByTags(List<String> tagNames) {
        return articleRepository.findByTagsAndPublishedTrue(tagNames);
    }

    /**
     * Поиск статей только по названию
     */
    @Transactional(readOnly = true)
    public List<Article> searchArticlesByTitle(String title) {
        return articleRepository.findByTitleContainingIgnoreCaseAndPublishedTrue(title.trim());
    }

    /**
     * Расширенный поиск с различными параметрами
     */
    @Transactional(readOnly = true)
    public List<Article> advancedSearch(String title, List<String> tags, String authorName, Boolean published) {
        if (tags != null && !tags.isEmpty()) {
            Set<Tag> tagEntities = tagNamesToTags(tags);
            if (title != null && !title.trim().isEmpty()) {
                return articleRepository.findByTitleContainingAndTagsInAndPublished(
                        title.trim(), tagEntities, published != null ? published : true);
            } else {
                return articleRepository.findByTagsInAndPublished(tagEntities, published != null ? published : true);
            }
        } else if (title != null && !title.trim().isEmpty()) {
            return articleRepository.findByTitleContainingIgnoreCaseAndPublished(
                    title.trim(), published != null ? published : true);
        } else if (authorName != null && !authorName.trim().isEmpty()) {
            return articleRepository.findByAuthorNameContainingIgnoreCaseAndPublished(
                    authorName.trim(), published != null ? published : true);
        } else {
            return articleRepository.findByPublished(published != null ? published : true);
        }
    }

    private Set<Tag> tagNamesToTags(List<String> tagNames) {
        return tagNames.stream()
                .map(tagName -> tagRepository.findByName(tagName.trim().toLowerCase()))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .collect(Collectors.toSet());
    }

    /**
     * Создание статьи с тегами
     */
    @Transactional
    public Article createArticle(String title, String shortDescription,
                                 List<com.postread.data.ArticleBlock> blocks,
                                 Long authorId, boolean isPublished, Set<String> tags) {
        Article article = new Article();
        article.setTitle(title);
        article.setShortDescription(shortDescription);
        article.setBlocks(blocks);
        article.setAuthor(userRepository.findById(authorId).get());
        article.setPublished(isPublished);
        article.setViewCount(0);

        // Обрабатываем теги
        if (tags != null && !tags.isEmpty()) {
            Set<Tag> tagEntities = tagService.findOrCreateTags(tags);
            article.setTags(tagEntities);
        }

        return articleRepository.save(article);
    }
}