package com.postread.controllers;

import com.postread.data.Article;
import com.postread.data.Bookmark;
import com.postread.repositories.ArticleRepository;
import com.postread.repositories.UserRepository;
import com.postread.security.User;
import com.postread.services.BookmarkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/user")
public class UserProfileController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private BookmarkService bookmarkService;

    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute("user") User userForm,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByName(username)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            // Проверяем уникальность email (если изменился)
            if (!user.getEmail().equals(userForm.getEmail())) {
                if (userRepository.existsUserByEmail(userForm.getEmail())) {
                    redirectAttributes.addFlashAttribute("error", "Email уже занят");
                    return "redirect:/user/profile";
                }
            }

            // Проверяем уникальность имени (если изменился)
            if (!user.getName().equals(userForm.getName())) {
                if (userRepository.existsUserByName(userForm.getName())) {
                    redirectAttributes.addFlashAttribute("error", "Имя пользователя уже занято");
                    return "redirect:/user/profile";
                }
            }

            // Обновляем данные пользователя
            user.setName(userForm.getName());
            user.setEmail(userForm.getEmail());
            user.setBirthDate(userForm.getBirthDate());
            user.setDescription(userForm.getDescription() != null ? userForm.getDescription() : "");
            user.setIcon(userForm.getIcon() != null ? userForm.getIcon() : "");

            userRepository.save(user);
            redirectAttributes.addFlashAttribute("success", "Профиль успешно обновлен");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при обновлении профиля: " + e.getMessage());
        }

        return "redirect:/user/profile";
    }

    @GetMapping("/profile")
    public String userProfile(Model model, Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Инициализируем null поля
        if (user.getDescription() == null) {
            user.setDescription("");
        }
        if (user.getIcon() == null) {
            user.setIcon("");
        }

        // Получаем статьи пользователя
        List<Article> userArticles = articleRepository.findByAuthorId(user.getId());
        List<Article> drafts = userArticles.stream()
                .filter(article -> !article.isPublished())
                .collect(Collectors.toList());
        List<Article> published = userArticles.stream()
                .filter(Article::isPublished)
                .collect(Collectors.toList());

        // Получаем избранные статьи
        List<Bookmark> bookmarks = bookmarkService.getUserBookmarks(user.getId());
        long bookmarksCount = bookmarkService.getBookmarksCount(user.getId());

        // Статистика
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalArticles", userArticles.size());
        stats.put("publishedArticles", published.size());
        stats.put("draftArticles", drafts.size());
        stats.put("totalViews", userArticles.stream().mapToInt(Article::getViewCount).sum());

        model.addAttribute("user", user);
        model.addAttribute("drafts", drafts);
        model.addAttribute("published", published);
        model.addAttribute("bookmarks", bookmarks);
        model.addAttribute("bookmarksCount", bookmarksCount);
        model.addAttribute("stats", stats);

        return "user-profile";
    }
}