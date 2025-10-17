package com.postread.services;

import com.postread.data.Article;
import com.postread.data.ArticleBlock;
import com.postread.security.User;
import com.postread.repositories.ArticleRepository;
import com.postread.repositories.ArticleBlockRepository;
import com.postread.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ArticleService {

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private ArticleBlockRepository blockRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public Article createArticle(String title, String shortDescription,
                                 List<ArticleBlock> blocks, Long authorId, boolean isPublished) {

        // Находим автора по ID
        Optional<User> authorOpt = userRepository.findById(authorId);
        if (authorOpt.isEmpty()) {
            throw new RuntimeException("Автор с ID " + authorId + " не найден");
        }
        User author = authorOpt.get();

        Article article = new Article();
        article.setTitle(title);
        article.setShortDescription(shortDescription);
        article.setPublished(isPublished);
        article.setAuthor(author);
        article.setCreatedAt(LocalDateTime.now());
        article.setUpdatedAt(LocalDateTime.now());
        article.setViewCount(0);

        // Сохраняем статью сначала чтобы получить ID
        Article savedArticle = articleRepository.save(article);

        // Устанавливаем связь для каждого блока и сохраняем
        if (blocks != null) {
            for (int i = 0; i < blocks.size(); i++) {
                ArticleBlock block = blocks.get(i);
                block.setArticle(savedArticle);
                block.setOrder(i);
                blockRepository.save(block);
            }
        }

        return savedArticle;
    }

    public List<Article> getAllPublishedArticles() {
        return articleRepository.findAllByPublishedTrueOrderByCreatedAtDesc();
    }

    public Optional<Article> getArticleById(Long id) {
        return articleRepository.findById(id);
    }
}