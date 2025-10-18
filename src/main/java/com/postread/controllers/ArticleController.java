package com.postread.controllers;

import com.postread.data.Article;
import com.postread.data.ArticleBlock;
import com.postread.data.Tag;
import com.postread.dto.ArticleBlockDTO;
import com.postread.dto.ArticleDTO;
import com.postread.security.User;
import com.postread.repositories.ArticleRepository;
import com.postread.repositories.UserRepository;
import com.postread.services.ArticleService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.postread.services.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Controller
@RequestMapping("/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final TagService tagService;

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
        List<Article> articles = articleRepository.findAllOriginalArticles();
        model.addAttribute("articles", articles);
        return "articles-list";
    }

    @GetMapping("/{id}")
    public String getArticle(@PathVariable Long id, Model model) {
        try {
            // Этот метод автоматически увеличивает счетчик просмотров
            ArticleDTO articleDTO = articleService.getArticleDTO(id);
            model.addAttribute("article", articleDTO);

            // Детальная отладочная информация
            System.out.println("=== ДЕТАЛЬНАЯ ИНФОРМАЦИЯ О СТАТЬЕ ===");
            System.out.println("Статья ID: " + articleDTO.getId());
            System.out.println("Заголовок: " + articleDTO.getTitle());
            System.out.println("Просмотры: " + articleDTO.getViewCount());
            System.out.println("Количество блоков: " +
                    (articleDTO.getBlocks() != null ? articleDTO.getBlocks().size() : 0));

            if (articleDTO.getBlocks() != null) {
                for (int i = 0; i < articleDTO.getBlocks().size(); i++) {
                    ArticleBlockDTO block = articleDTO.getBlocks().get(i);
                    String contentPreview = block.getContent() != null ?
                            block.getContent().substring(0, Math.min(50, block.getContent().length())) : "null";
                    System.out.println("Блок " + i + ": ID=" + block.getId() +
                            ", тип=" + block.getType() +
                            ", порядок=" + block.getOrder() +
                            ", контент=" + contentPreview);
                }
            } else {
                System.out.println("Блоки: null");
            }
            System.out.println("=== КОНЕЦ ДЕТАЛЬНОЙ ИНФОРМАЦИИ ===");

            return "article";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Статья не найдена: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> getArticleApi(@PathVariable Long id) {
        try {
            // API endpoint тоже увеличивает счетчик просмотров
            ArticleDTO articleDTO = articleService.getArticleDTO(id);
            return ResponseEntity.ok(articleDTO);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/editor")
    public String showEditor() {
        try {
            getCurrentUser();
            return "article-editor";
        } catch (RuntimeException e) {
            return "redirect:/auth/login";
        }
    }

    @GetMapping("/editor/{articleId}")
    public String showReviewEditor(@PathVariable Long articleId, Model model) {
        try {
            getCurrentUser();
            Optional<Article> article = articleRepository.findById(articleId);
            if (article.isPresent()) {
                model.addAttribute("originalArticle", article.get());
                return "review-editor";
            }
            return "redirect:/articles";
        } catch (RuntimeException e) {
            return "redirect:/auth/login";
        }
    }

    @GetMapping("/create-form")
    public String showCreateForm() {
        try {
            getCurrentUser();
            return "create-article";
        } catch (RuntimeException e) {
            return "redirect:/auth/login";
        }
    }

    @GetMapping("/create-review-form")
    public String showCreateReviewForm() {
        try {
            getCurrentUser();
            return "create-review";
        } catch (RuntimeException e) {
            return "redirect:/auth/login";
        }
    }

    @GetMapping("/{id}/reviews")
    public String getArticleReviews(@PathVariable Long id, Model model) {
        Optional<Article> articleOpt = articleRepository.findById(id);
        if (articleOpt.isPresent()) {
            Article article = articleOpt.get();
            List<Article> reviews = articleService.getReviewsForArticle(id);

            model.addAttribute("article", article);
            model.addAttribute("reviews", reviews);
            return "reviews-list";
        }
        return "redirect:/articles";
    }

    @GetMapping("/search")
    public String showSearchPage(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) String searchType,
            Model model) {

        String searchTitleValue = title != null ? title : "";
        String searchTagsValue = "";
        String searchTypeValue = searchType != null ? searchType : "all";

        boolean hasSearchParams = (title != null && !title.trim().isEmpty()) ||
                (tags != null && !tags.isEmpty());

        if (hasSearchParams) {
            List<Article> articles;

            try {
                if ("tags".equals(searchType) && tags != null && !tags.isEmpty()) {
                    articles = articleService.searchArticlesByTags(tags);
                } else if ("title".equals(searchType) && title != null && !title.trim().isEmpty()) {
                    articles = articleService.searchArticlesByTitle(title);
                } else {
                    articles = articleService.searchArticlesByTitleAndTags(title, tags);
                }

                model.addAttribute("articles", articles);
                model.addAttribute("resultsCount", articles.size());

            } catch (Exception e) {
                model.addAttribute("error", "Ошибка при поиске: " + e.getMessage());
                model.addAttribute("articles", new ArrayList<>());
                model.addAttribute("resultsCount", 0);
            }

            if (tags != null && !tags.isEmpty()) {
                searchTagsValue = String.join(", ", tags);
            }
        } else {
            model.addAttribute("articles", new ArrayList<>());
            model.addAttribute("resultsCount", 0);
        }

        model.addAttribute("searchTitle", searchTitleValue);
        model.addAttribute("searchTags", searchTagsValue);
        model.addAttribute("searchType", searchTypeValue);
        model.addAttribute("hasSearchParams", hasSearchParams);

        return "search";
    }

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

    @GetMapping("/api/user/current")
    @ResponseBody
    public ResponseEntity<?> getCurrentUserInfo() {
        try {
            User currentUser = getCurrentUser();
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", currentUser.getId());
            userInfo.put("username", currentUser.getName());
            userInfo.put("email", currentUser.getEmail());
            return ResponseEntity.ok(userInfo);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    // Обновленный метод createArticle для поддержки рецензий
    @PostMapping("/create")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> createArticle(
            @RequestParam String title,
            @RequestParam String shortDescription,
            @RequestParam String blocks,
            @RequestParam(required = false) Long authorId,
            @RequestParam boolean isPublished,
            @RequestParam(required = false) Set<String> tags,
            @RequestParam(required = false) Long reviewForArticleId) {

        try {
            User currentUser = getCurrentUser();
            Long actualAuthorId = currentUser.getId();

            if (authorId != null && !currentUser.getId().equals(authorId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Вы можете создавать статьи только от своего имени");
            }

            // Проверяем, не написал ли пользователь уже рецензию на эту статью
            if (reviewForArticleId != null) {
                if (articleService.hasUserReviewedArticle(currentUser, reviewForArticleId)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Вы уже написали рецензию на эту статью");
                }
            }

            List<ArticleBlock> articleBlocks = parseBlocksFromJson(blocks);

            Article article = articleService.createArticle(
                    title,
                    shortDescription,
                    articleBlocks,
                    actualAuthorId,
                    isPublished,
                    tags,
                    reviewForArticleId
            );

            // Перезагружаем статью без ленивых коллекций для безопасной сериализации
            Article freshArticle = articleRepository.findById(article.getId())
                    .orElseThrow(() -> new RuntimeException("Статья не найдена после создания"));

            // Создаем упрощенный объект для ответа
            Map<String, Object> response = new HashMap<>();
            response.put("id", freshArticle.getId());
            response.put("title", freshArticle.getTitle());
            response.put("shortDescription", freshArticle.getShortDescription());
            response.put("published", freshArticle.isPublished());
            response.put("createdAt", freshArticle.getCreatedAt());
            response.put("updatedAt", freshArticle.getUpdatedAt());
            response.put("viewCount", freshArticle.getViewCount());

            // Информация об авторе
            if (freshArticle.getAuthor() != null) {
                Map<String, Object> authorInfo = new HashMap<>();
                authorInfo.put("id", freshArticle.getAuthor().getId());
                authorInfo.put("name", freshArticle.getAuthor().getName());
                response.put("author", authorInfo);
            }

            // Информация о рецензии (если это рецензия)
            if (freshArticle.getReviewForArticle() != null) {
                Map<String, Object> reviewInfo = new HashMap<>();
                reviewInfo.put("id", freshArticle.getReviewForArticle().getId());
                reviewInfo.put("title", freshArticle.getReviewForArticle().getTitle());
                response.put("reviewForArticle", reviewInfo);
            }

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Ошибка аутентификации: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
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