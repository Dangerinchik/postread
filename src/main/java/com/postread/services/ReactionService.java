package com.postread.services;

import com.postread.data.Article;
import com.postread.data.Reaction;
import com.postread.data.ReactionType;
import com.postread.repositories.ArticleRepository;
import com.postread.repositories.ReactionRepository;
import com.postread.security.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ReactionService {

    @Autowired
    private ReactionRepository reactionRepository;

    @Autowired
    private ArticleRepository articleRepository;

    /**
     * Добавить или изменить реакцию пользователя
     */
    @Transactional
    public Reaction addOrUpdateReaction(User user, Long articleId, ReactionType reactionType) {
        // Получаем существующую статью из базы данных
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Статья не найдена с id: " + articleId));

        // Проверяем, есть ли уже реакция от этого пользователя
        Optional<Reaction> existingReaction = reactionRepository.findByUserAndArticle(user, article);

        if (existingReaction.isPresent()) {
            Reaction reaction = existingReaction.get();
            reaction.setType(reactionType.getCode());
            return reactionRepository.save(reaction);
        } else {
            Reaction newReaction = new Reaction();
            newReaction.setUser(user);
            newReaction.setArticle(article);
            newReaction.setType(reactionType.getCode());
            return reactionRepository.save(newReaction);
        }
    }

    /**
     * Удалить реакцию пользователя
     */
    @Transactional
    public void removeReaction(User user, Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Статья не найдена с id: " + articleId));
        reactionRepository.deleteByUserAndArticle(user, article);
    }

    /**
     * Получить реакцию пользователя для статьи
     */
    public Optional<Reaction> getUserReaction(User user, Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Статья не найдена с id: " + articleId));
        return reactionRepository.findByUserAndArticle(user, article);
    }

    /**
     * Получить статистику реакций для статьи
     */
    public Map<ReactionType, Long> getReactionStats(Long articleId) {
        List<Object[]> results = reactionRepository.getReactionCountsByArticleId(articleId);
        Map<ReactionType, Long> stats = new HashMap<>();

        for (Object[] result : results) {
            Integer typeCode = (Integer) result[0];
            Long count = (Long) result[1];
            try {
                ReactionType type = ReactionType.fromCode(typeCode);
                stats.put(type, count);
            } catch (IllegalArgumentException e) {
                // Игнорируем неизвестные коды реакций
                System.err.println("Неизвестный код реакции: " + typeCode);
            }
        }

        // Инициализируем все типы реакций с нулевым счетчиком
        for (ReactionType type : ReactionType.values()) {
            stats.putIfAbsent(type, 0L);
        }

        return stats;
    }

    /**
     * Получить общее количество реакций для статьи
     */
    public long getTotalReactionsCount(Long articleId) {
        return reactionRepository.findByArticleId(articleId).size();
    }

    /**
     * Проверить, поставил ли пользователь реакцию
     */
    public boolean hasUserReacted(User user, Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Статья не найдена с id: " + articleId));
        return reactionRepository.existsByUserAndArticle(user, article);
    }
}