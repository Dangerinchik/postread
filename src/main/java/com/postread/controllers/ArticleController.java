package com.postread.controllers;

import com.postread.data.Article;
import com.postread.data.ArticleBlock;
import com.postread.data.Tag;
import com.postread.security.User;
import com.postread.repositories.ArticleRepository;
import com.postread.repositories.UserRepository;
import com.postread.services.ArticleService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.postread.services.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    @Autowired
    private TagService tagService;

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

    @GetMapping("/search")
    public String showSearchPage(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) String searchType,
            Model model) {

        // Инициализируем пустые значения по умолчанию
        String searchTitleValue = title != null ? title : "";
        String searchTagsValue = "";
        String searchTypeValue = searchType != null ? searchType : "all";

        // Если есть параметры поиска, выполняем поиск
        boolean hasSearchParams = (title != null && !title.trim().isEmpty()) ||
                (tags != null && !tags.isEmpty());

        if (hasSearchParams) {
            List<Article> articles;

            try {
                if ("tags".equals(searchType) && tags != null && !tags.isEmpty()) {
                    // Поиск только по тегам
                    articles = articleService.searchArticlesByTags(tags);
                } else if ("title".equals(searchType) && title != null && !title.trim().isEmpty()) {
                    // Поиск только по названию
                    articles = articleService.searchArticlesByTitle(title);
                } else {
                    // Комбинированный поиск или поиск по умолчанию
                    articles = articleService.searchArticlesByTitleAndTags(title, tags);
                }

                model.addAttribute("articles", articles);
                model.addAttribute("resultsCount", articles.size());

            } catch (Exception e) {
                model.addAttribute("error", "Ошибка при поиске: " + e.getMessage());
                model.addAttribute("articles", new ArrayList<>());
                model.addAttribute("resultsCount", 0);
            }

            // Формируем строку тегов для отображения
            if (tags != null && !tags.isEmpty()) {
                searchTagsValue = String.join(", ", tags);
            }
        } else {
            // Если нет параметров поиска, просто показываем пустую страницу
            model.addAttribute("articles", new ArrayList<>());
            model.addAttribute("resultsCount", 0);
        }

        model.addAttribute("searchTitle", searchTitleValue);
        model.addAttribute("searchTags", searchTagsValue);
        model.addAttribute("searchType", searchTypeValue);
        model.addAttribute("hasSearchParams", hasSearchParams);

        return "search";
    }

    /**
     * API для поиска статей (для AJAX)
     */
    @GetMapping("/api/search")
    @ResponseBody
    public ResponseEntity<?> searchArticlesApi(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false, defaultValue = "all") String searchType) {

        try {
            List<Article> articles;

            if ("tags".equals(searchType) && tags != null && !tags.isEmpty()) {
                articles = articleService.searchArticlesByTags(tags);
            } else if ("title".equals(searchType) && title != null && !title.trim().isEmpty()) {
                articles = articleService.searchArticlesByTitle(title);
            } else {
                articles = articleService.searchArticlesByTitleAndTags(title, tags);
            }

            return ResponseEntity.ok(articles);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка при поиске: " + e.getMessage());
        }
    }

    /**
     * Расширенный поиск (отдельная страница)
     */
    @GetMapping("/search-advanced")
    public String advancedSearch(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Boolean published,
            Model model) {

        try {
            List<Article> articles = articleService.advancedSearch(title, tags, author, published);

            model.addAttribute("articles", articles);
            model.addAttribute("searchTitle", title != null ? title : "");
            model.addAttribute("searchTags", tags != null ? String.join(",", tags) : "");
            model.addAttribute("searchAuthor", author != null ? author : "");
            model.addAttribute("searchPublished", published);
            model.addAttribute("resultsCount", articles.size());

        } catch (Exception e) {
            model.addAttribute("error", "Ошибка при поиске: " + e.getMessage());
            model.addAttribute("articles", new ArrayList<>());
            model.addAttribute("resultsCount", 0);
        }

        return "search-advanced";
    }

    @GetMapping("/api/tags")
    @ResponseBody
    public ResponseEntity<List<Tag>> searchTags(@RequestParam(required = false) String query) {
        try {
            List<Tag> tags;
            if (query == null || query.trim().isEmpty()) {
                tags = tagService.getPopularTags(10);
            } else {
                tags = tagService.searchTags(query);
            }
            return ResponseEntity.ok(tags);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Обновленный метод createArticle для поддержки тегов
    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<?> createArticle(
            @RequestParam String title,
            @RequestParam String shortDescription,
            @RequestParam String blocks,
            @RequestParam(required = false) Long authorId,
            @RequestParam boolean isPublished,
            @RequestParam(required = false) Set<String> tags) {

        try {
            User currentUser = getCurrentUser();
            Long actualAuthorId = currentUser.getId();

            if (authorId != null && !currentUser.getId().equals(authorId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Вы можете создавать статьи только от своего имени");
            }

            List<ArticleBlock> articleBlocks = parseBlocksFromJson(blocks);

            Article article = articleService.createArticle(
                    title,
                    shortDescription,
                    articleBlocks,
                    actualAuthorId,
                    isPublished,
                    tags
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

    private List<ArticleBlock> parseBlocksFromJson(String blocksJson) {
        try {
            return objectMapper.readValue(blocksJson, new TypeReference<List<ArticleBlock>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при парсинге блоков: " + e.getMessage());
        }
    }
}