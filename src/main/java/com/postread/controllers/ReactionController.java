package com.postread.controllers;

import com.postread.data.Reaction;
import com.postread.data.ReactionType;
import com.postread.security.User;
import com.postread.services.ReactionService;
import com.postread.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/api/reactions")
public class ReactionController {

    @Autowired
    private ReactionService reactionService;

    @Autowired
    private UserRepository userRepository;

    // Маппинг эмодзи на коды реакций
    private final Map<String, Integer> emojiToCode = Map.ofEntries(
            Map.entry("👍", 1), Map.entry("👎", 2), Map.entry("❤️", 3), Map.entry("😂", 4),
            Map.entry("😢", 5), Map.entry("😠", 6), Map.entry("😮", 7), Map.entry("🔥", 8),
            Map.entry("🤔", 9), Map.entry("👏", 10), Map.entry("😕", 11), Map.entry("🎉", 12)
    );

    // Маппинг кодов на эмодзи
    private final Map<Integer, String> codeToEmoji = Map.ofEntries(
            Map.entry(1, "👍"), Map.entry(2, "👎"), Map.entry(3, "❤️"), Map.entry(4, "😂"),
            Map.entry(5, "😢"), Map.entry(6, "😠"), Map.entry(7, "😮"), Map.entry(8, "🔥"),
            Map.entry(9, "🤔"), Map.entry(10, "👏"), Map.entry(11, "😕"), Map.entry(12, "🎉")
    );

    // Получаем текущего аутентифицированного пользователя (такой же метод как в ArticleController)
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

    @PostMapping("/article/{articleId}")
    public ResponseEntity<?> addReaction(
            @PathVariable Long articleId,
            @RequestParam String emoji) {

        try {
            // Получаем текущего пользователя через SecurityContextHolder
            User user = getCurrentUser();

            Integer reactionCode = emojiToCode.get(emoji);
            if (reactionCode == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("Неизвестная реакция: " + emoji));
            }

            ReactionType reactionType = ReactionType.fromCode(reactionCode);
            Reaction reaction = reactionService.addOrUpdateReaction(user, articleId, reactionType);

            // Получаем обновленную статистику
            Map<ReactionType, Long> stats = reactionService.getReactionStats(articleId);
            Integer userReaction = reaction.getType();

            return ResponseEntity.ok(createSuccessResponse(stats, userReaction));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(createErrorResponse("Внутренняя ошибка сервера"));
        }
    }

    @DeleteMapping("/article/{articleId}")
    public ResponseEntity<?> removeReaction(@PathVariable Long articleId) {

        try {
            // Получаем текущего пользователя через SecurityContextHolder
            User user = getCurrentUser();

            reactionService.removeReaction(user, articleId);

            // Получаем обновленную статистику
            Map<ReactionType, Long> stats = reactionService.getReactionStats(articleId);

            return ResponseEntity.ok(createSuccessResponse(stats, null));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(createErrorResponse("Ошибка при удалении реакции: " + e.getMessage()));
        }
    }

    @GetMapping("/article/{articleId}/stats")
    public ResponseEntity<?> getReactionStats(@PathVariable Long articleId) {

        try {
            Map<ReactionType, Long> stats = reactionService.getReactionStats(articleId);
            Integer userReaction = null;

            // Получаем текущего пользователя через SecurityContextHolder
            try {
                User user = getCurrentUser();
                userReaction = reactionService.getUserReaction(user, articleId)
                        .map(Reaction::getType)
                        .orElse(null);
            } catch (RuntimeException e) {
                // Пользователь не аутентифицирован - это нормально для статистики
                userReaction = null;
            }

            return ResponseEntity.ok(createSuccessResponse(stats, userReaction));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(createErrorResponse("Ошибка при получении статистики: " + e.getMessage()));
        }
    }

    private Map<String, Object> createSuccessResponse(Map<ReactionType, Long> stats, Integer userReaction) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);

        // Конвертируем статистику в формат для фронтенда
        Map<String, Long> formattedStats = new HashMap<>();
        for (Map.Entry<ReactionType, Long> entry : stats.entrySet()) {
            String emoji = codeToEmoji.get(entry.getKey().getCode());
            if (emoji != null && entry.getValue() > 0) {
                formattedStats.put(emoji, entry.getValue());
            }
        }
        response.put("stats", formattedStats);
        response.put("userReaction", userReaction);

        return response;
    }

    private Map<String, Object> createErrorResponse(String error) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", error);
        return response;
    }
}