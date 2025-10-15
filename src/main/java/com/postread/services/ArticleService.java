package com.postread.services;

import com.postread.data.Article;
import com.postread.data.User;
import com.postread.dto.ArticleDto;
import com.postread.repositories.ArticleRepository;
import com.postread.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ArticleService {
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    @Transactional
    public Article createArticle (ArticleDto request) {
        User author = userRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new RuntimeException("Автор не найден с id: " + request.getAuthorId()));

        Article article = new Article();
        article.setAuthor(author);
        article.setTitle(request.getTitle());
        article.setShortDescription(request.getShortDescription());
        article.setText(request.getText());
        article.setPublished(request.isPublished());
        article.setViewCount(0);

        return articleRepository.save(article);
    }

    public Optional<Article> findByTitle(String title) {
        return articleRepository.findByTitle(title);
    }
}
