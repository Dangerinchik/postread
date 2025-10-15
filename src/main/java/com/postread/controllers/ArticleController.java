package com.postread.controllers;

import com.postread.data.Article;
import com.postread.dto.ArticleDto;
import com.postread.services.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @GetMapping("/editor")
    public String showEditor(){
        return "editor";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model){
        model.addAttribute("article", new ArticleDto());
        return "createArticle";
    }

    @PostMapping("/save")
    public String saveArticle(@ModelAttribute ArticleDto request){
        Article createdArticle = articleService.createArticle(request);
        return "editor";
    }
    @GetMapping("/by-title")
    public String getArticleByTitle(@RequestParam String title, Model model) {
        Optional<Article> article = articleService.findByTitle(title);

        if (article.isPresent()) {
            model.addAttribute("article", article.get());
            return "articleDetail";
        } else {
            model.addAttribute("error", "Статья с названием '" + title + "' не найдена");
            return "articleNotFound";
        }
    }

}
