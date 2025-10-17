package com.postread.controllers;

import com.postread.data.Article;
import com.postread.data.ArticleBlock;
import com.postread.security.User;
import com.postread.repositories.ArticleRepository;
import com.postread.repositories.UserRepository;
import com.postread.services.ArticleService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/articles")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // Получаем текущего аутентифицированного пользователя
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser")) {
            String username = authentication.getName();
            return userRepository.findByName(username)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + username));
        }
        throw new RuntimeException("Пользователь не аутентифицирован");
    }

    @GetMapping
    public String getAllArticles(Model model) {
        List<Article> articles = articleRepository.findAllByPublishedTrueOrderByCreatedAtDesc();
        model.addAttribute("articles", articles);
        return "articles-list";
    }

    @GetMapping("/{id}")
    public String getArticle(@PathVariable Long id, Model model) {
        Optional<Article> articleOpt = articleRepository.findById(id);
        if (articleOpt.isPresent()) {
            Article article = articleOpt.get();
            // Увеличиваем счетчик просмотров
            article.setViewCount(article.getViewCount() + 1);
            articleRepository.save(article);

            model.addAttribute("article", article);
            return "article";
        }
        return "redirect:/articles";
    }

    @GetMapping("/editor")
    public String showEditor() {
        // Проверяем аутентификацию
        try {
            getCurrentUser();
            return "article-editor";
        } catch (RuntimeException e) {
            return "redirect:/auth/login";
        }
    }

    @GetMapping("/create-form")
    public String showCreateForm() {
        // Проверяем аутентификацию
        try {
            getCurrentUser();
            return "create-article";
        } catch (RuntimeException e) {
            return "redirect:/auth/login";
        }
    }

    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<?> createArticle(
            @RequestParam String title,
            @RequestParam String shortDescription,
            @RequestParam String blocks,
            @RequestParam(required = false) Long authorId, // делаем необязательным
            @RequestParam boolean isPublished) {

        try {
            User currentUser = getCurrentUser();

            // Используем ID текущего пользователя, а не переданный параметр
            Long actualAuthorId = currentUser.getId();

            // Если authorId передан и не совпадает с текущим пользователем - ошибка
            if (authorId != null && !currentUser.getId().equals(authorId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Вы можете создавать статьи только от своего имени");
            }

            // Преобразуем JSON строку блоков в список
            List<ArticleBlock> articleBlocks = parseBlocksFromJson(blocks);

            Article article = articleService.createArticle(
                    title,
                    shortDescription,
                    articleBlocks,
                    actualAuthorId, // используем ID текущего пользователя
                    isPublished
            );

            return ResponseEntity.ok(article);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Ошибка аутентификации: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка при создании статьи: " + e.getMessage());
        }
    }

    @GetMapping("/search")
    public String showSearchPage() {
        return "search";
    }

    @GetMapping("/api/search")
    @ResponseBody
    public List<Article> searchArticles(@RequestParam String title) {
        return articleRepository.findByTitleContainingIgnoreCaseAndPublishedTrue(title);
    }

    // Добавляем endpoint для получения информации о текущем пользователе
    @GetMapping("/api/user/current")
    @ResponseBody
    public ResponseEntity<?> getCurrentUserInfo() {
        try {
            User currentUser = getCurrentUser();
            java.util.Map<String, Object> userInfo = new java.util.HashMap<>();
            userInfo.put("id", currentUser.getId());
            userInfo.put("username", currentUser.getName());
            userInfo.put("email", currentUser.getEmail());
            return ResponseEntity.ok(userInfo);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Пользователь не аутентифицирован");
        }
    }

    private List<ArticleBlock> parseBlocksFromJson(String blocksJson) {
        try {
            return objectMapper.readValue(blocksJson, new TypeReference<List<ArticleBlock>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при парсинге блоков: " + e.getMessage());
        }
    }
}