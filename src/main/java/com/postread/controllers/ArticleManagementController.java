package com.postread.controllers;

import com.postread.data.Article;
import com.postread.data.ArticleBlock;
import com.postread.dto.ArticleDTO;
import com.postread.security.User;
import com.postread.repositories.UserRepository;
import com.postread.services.ArticleManagementService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/articles")
@RequiredArgsConstructor
public class ArticleManagementController {

    private final ArticleManagementService articleManagementService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

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

    /**
     * Страница редактирования статьи
     */
    @GetMapping("/edit/{id}")
    public String editArticle(@PathVariable Long id, Model model) {
        try {
            User currentUser = getCurrentUser();
            ArticleDTO articleDTO = articleManagementService.getArticleDTOForEditing(id, currentUser.getId());

            model.addAttribute("article", articleDTO);
            model.addAttribute("isEdit", true);

            return "article-editor2";
        } catch (RuntimeException e) {
            return "redirect:/user/profile?error=" + e.getMessage();
        }
    }

    @PostMapping("/update/{id}")
    @ResponseBody
    public ResponseEntity<?> updateArticle(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam String shortDescription,
            @RequestParam String blocks,
            @RequestParam boolean isPublished,
            @RequestParam(required = false) Set<String> tags) {

        try {
            User currentUser = getCurrentUser();

            List<ArticleBlock> articleBlocks = parseBlocksFromJson(blocks);

            // Убедимся, что у всех блоков правильный порядок
            for (int i = 0; i < articleBlocks.size(); i++) {
                articleBlocks.get(i).setOrder(i);
            }

            // Используем простой и надежный метод
            Article updatedArticle = articleManagementService.updateArticleSimple(
                    id, currentUser.getId(), title, shortDescription,
                    articleBlocks, isPublished, tags
            );

            // Сохраняем данные статьи в sessionStorage для передачи на create-article
            Map<String, Object> articleData = new HashMap<>();
            articleData.put("id", updatedArticle.getId());
            articleData.put("title", updatedArticle.getTitle());
            articleData.put("shortDescription", updatedArticle.getShortDescription());
            articleData.put("isPublished", updatedArticle.isPublished());
            articleData.put("blocks", articleBlocks);
            articleData.put("tags", tags != null ? tags : Set.of());

            // Создаем ответ с данными для передачи
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Статья успешно обновлена");
            response.put("articleData", articleData);
            response.put("redirectUrl", "/articles/create-form?edit=true&articleId=" + updatedArticle.getId());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Ошибка при обновлении статьи: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Удаление статьи - ИСПРАВЛЕННАЯ ВЕРСИЯ
     */
    @PostMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteArticle(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            articleManagementService.deleteArticle(id, currentUser.getId());

            // ИСПРАВЛЕННАЯ ВЕРСИЯ - используем HashMap вместо Map.of()
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Статья успешно удалена");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Ошибка при удалении статьи: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Публикация статьи
     */
    @PostMapping("/publish/{id}")
    @ResponseBody
    public ResponseEntity<?> publishArticle(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            Article article = articleManagementService.publishArticle(id, currentUser.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Статья успешно опубликована");
            response.put("articleId", article.getId());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Ошибка при публикации статьи: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Перевод статьи в черновики
     */
    @PostMapping("/unpublish/{id}")
    @ResponseBody
    public ResponseEntity<?> unpublishArticle(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            Article article = articleManagementService.unpublishArticle(id, currentUser.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Статья переведена в черновики");
            response.put("articleId", article.getId());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Ошибка при переводе статьи в черновики: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private List<ArticleBlock> parseBlocksFromJson(String blocksJson) {
        try {
            return objectMapper.readValue(blocksJson, new TypeReference<List<ArticleBlock>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при парсинге блоков: " + e.getMessage());
        }
    }

    /**
     * Страница создания/редактирования статьи с параметрами
     */
    @GetMapping("/create-form2")
    public String createArticleForm(@RequestParam(required = false) Boolean edit,
                                    @RequestParam(required = false) Long articleId,
                                    Model model) {
        try {
            User currentUser = getCurrentUser();

            if (edit != null && edit && articleId != null) {
                // Режим редактирования - загружаем данные статьи
                ArticleDTO articleDTO = articleManagementService.getArticleDTOForEditing(articleId, currentUser.getId());
                model.addAttribute("article", articleDTO);
                model.addAttribute("isEdit", true);
            } else {
                // Режим создания новой статьи
                model.addAttribute("isEdit", false);
            }

            return "create-article";
        } catch (RuntimeException e) {
            return "redirect:/articles/editor?error=" + e.getMessage();
        }
    }
}