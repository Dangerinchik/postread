package com.postread.controllers;

import com.postread.data.Article;
import com.postread.services.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/articles")
@RequiredArgsConstructor
public class ArticleViewController {
    private final ArticleService articleService;

    // Главная страница со списком статей
    @GetMapping
    public String getAllArticles(Model model) {
        List<Article> articles = articleService.getPublishedArticles();
        model.addAttribute("articles", articles);
        return "articles-list"; // или "index" если хочешь главную страницу
    }

    // Страница редактора блоков
    @GetMapping("/editor")
    public String getEditor() {
        return "article-editor";
    }

    // Форма создания статьи (второй шаг)
    @GetMapping("/create-form")
    public String createArticleForm() {
        return "create-article";
    }

    // Просмотр конкретной статьи
    @GetMapping("/{id}")
    public String getArticle(@PathVariable Long id, Model model) {
        Article article = articleService.getArticleWithBlocks(id);
        model.addAttribute("article", article);
        return "article";
    }

    // Страница поиска
    @GetMapping("/search")
    public String searchArticles() {
        return "search";
    }
}