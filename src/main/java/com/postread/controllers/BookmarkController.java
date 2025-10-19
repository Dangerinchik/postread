package com.postread.controllers;

import com.postread.data.Bookmark;
import com.postread.security.User;
import com.postread.services.BookmarkService;
import com.postread.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;
    private final UserRepository userRepository;

    // Получаем текущего аутентифицированного пользователя (как в ArticleController)
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser")) {
            String username = authentication.getName();
            System.out.println("🔍 BookmarkController: Getting user by username: " + username);
            User user = userRepository.findByName(username)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + username));
            System.out.println("✅ BookmarkController: User found: " + user.getName() + " (ID: " + user.getId() + ")");
            return user;
        }
        throw new RuntimeException("Пользователь не аутентифицирован");
    }

    @PostMapping("/toggle/{articleId}")
    @ResponseBody
    public ResponseEntity<?> toggleBookmark(@PathVariable Long articleId) {
        try {
            System.out.println("🔄 BookmarkController: Toggle bookmark called for article: " + articleId);

            User currentUser = getCurrentUser();
            System.out.println("👤 Current user: " + currentUser.getName() + " (ID: " + currentUser.getId() + ")");

            // Прямой вызов без лишних проверок
            boolean isBookmarked = bookmarkService.toggleBookmark(currentUser.getId(), articleId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("bookmarked", isBookmarked);
            response.put("message", isBookmarked ?
                    "Статья добавлена в избранное" : "Статья удалена из избранного");

            System.out.println("✅ BookmarkController: Toggle completed - bookmarked: " + isBookmarked);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ BookmarkController ERROR: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Ошибка: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // API: Проверить статус избранного
    @GetMapping("/status/{articleId}")
    @ResponseBody
    public ResponseEntity<?> getBookmarkStatus(@PathVariable Long articleId) {
        try {
            System.out.println("🔍 BookmarkController: Getting bookmark status for article: " + articleId);

            User currentUser = getCurrentUser();
            Long userId = currentUser.getId();

            boolean isBookmarked = bookmarkService.isArticleBookmarked(userId, articleId);

            Map<String, Object> response = new HashMap<>();
            response.put("bookmarked", isBookmarked);
            response.put("userId", userId);
            response.put("articleId", articleId);

            System.out.println("✅ BookmarkController: Bookmark status - article " + articleId + ": " + isBookmarked);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            System.err.println("❌ BookmarkController: Authentication error in getBookmarkStatus: " + e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("bookmarked", false);
            response.put("error", "Not authenticated");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("❌ BookmarkController: Error getting bookmark status: " + e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("bookmarked", false);
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    // Страница с избранными статьями
    @GetMapping
    public String getBookmarksPage(Model model) {
        try {
            System.out.println("📖 BookmarkController: Loading bookmarks page");

            User currentUser = getCurrentUser();
            Long userId = currentUser.getId();

            List<Bookmark> bookmarks = bookmarkService.getUserBookmarks(userId);
            long bookmarksCount = bookmarkService.getBookmarksCount(userId);

            model.addAttribute("user", currentUser);
            model.addAttribute("bookmarks", bookmarks);
            model.addAttribute("bookmarksCount", bookmarksCount);

            System.out.println("✅ BookmarkController: Loaded " + bookmarksCount + " bookmarks for user " + currentUser.getName());
            return "bookmarks";
        } catch (RuntimeException e) {
            System.err.println("❌ BookmarkController: Authentication error - redirecting to login");
            return "redirect:/auth/login";
        } catch (Exception e) {
            System.err.println("❌ BookmarkController: Error loading bookmarks page: " + e.getMessage());
            model.addAttribute("error", "Ошибка при загрузке избранного: " + e.getMessage());
            return "bookmarks";
        }
    }
}