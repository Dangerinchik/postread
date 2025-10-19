package com.postread.services;

import com.postread.data.Bookmark;
import com.postread.data.Article;
import com.postread.security.User;
import com.postread.repositories.BookmarkRepository;
import com.postread.repositories.ArticleRepository;
import com.postread.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    // Добавить статью в избранное
    @Transactional
    public boolean addBookmark(Long userId, Long articleId) {
        try {
            System.out.println("➕ BookmarkService: Adding bookmark - User ID: " + userId + ", Article ID: " + articleId);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
            Article article = articleRepository.findById(articleId)
                    .orElseThrow(() -> new RuntimeException("Статья не найдена"));

            // Проверяем, не добавлена ли уже статья в избранное
            if (bookmarkRepository.existsByUserAndArticle(user.getId(), article.getId())) {
                System.out.println("ℹ️ BookmarkService: Bookmark already exists");
                return false;
            }

            Bookmark bookmark = new Bookmark();
            bookmark.setUser(user);
            bookmark.setArticle(article);

            bookmarkRepository.save(bookmark);
            System.out.println("✅ BookmarkService: Bookmark added successfully");
            return true;
        } catch (Exception e) {
            System.err.println("❌ BookmarkService: Error adding bookmark: " + e.getMessage());
            throw new RuntimeException("Ошибка при добавлении в избранное: " + e.getMessage());
        }
    }

    // Удалить статью из избранного
    @Transactional
    public boolean removeBookmark(Long userId, Long articleId) {
        try {
            System.out.println("➖ BookmarkService: Removing bookmark - User ID: " + userId + ", Article ID: " + articleId);

            Optional<Bookmark> bookmark = bookmarkRepository.findByUserAndArticle(
                    userRepository.findById(userId).orElse(null),
                    articleRepository.findById(articleId).orElse(null)
            );

            if (bookmark.isPresent()) {
                bookmarkRepository.delete(bookmark.get());
                System.out.println("✅ BookmarkService: Bookmark removed successfully");
                return true;
            }
            System.out.println("ℹ️ BookmarkService: Bookmark not found");
            return false;
        } catch (Exception e) {
            System.err.println("❌ BookmarkService: Error removing bookmark: " + e.getMessage());
            throw new RuntimeException("Ошибка при удалении из избранного: " + e.getMessage());
        }
    }

    // Проверить, добавлена ли статья в избранное (без выбрасывания исключений)
    public boolean isArticleBookmarked(Long userId, Long articleId) {
        try {
            System.out.println("🔍 BookmarkService: Checking bookmark status - User ID: " + userId + ", Article ID: " + articleId);

            User user = userRepository.findById(userId).orElse(null);
            Article article = articleRepository.findById(articleId).orElse(null);

            if (user == null || article == null) {
                System.out.println("⚠️ BookmarkService: User or article not found");
                return false;
            }

            boolean exists = bookmarkRepository.existsByUserAndArticle(user.getId(), article.getId());
            System.out.println("✅ BookmarkService: Bookmark exists: " + exists);
            return exists;
        } catch (Exception e) {
            System.err.println("❌ BookmarkService: Error checking bookmark status: " + e.getMessage());
            return false;
        }
    }

    // Получить все избранные статьи пользователя
    public List<Bookmark> getUserBookmarks(Long userId) {
        System.out.println("📚 BookmarkService: Getting user bookmarks for user ID: " + userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        List<Bookmark> bookmarks = bookmarkRepository.findByUserWithArticles(user);
        System.out.println("✅ BookmarkService: Found " + bookmarks.size() + " bookmarks");
        return bookmarks;
    }

    // Получить количество избранных статей
    public long getBookmarksCount(Long userId) {
        System.out.println("🔢 BookmarkService: Getting bookmarks count for user ID: " + userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        long count = bookmarkRepository.countByUser(user);
        System.out.println("✅ BookmarkService: Bookmarks count: " + count);
        return count;
    }

    @Transactional
    public boolean toggleBookmark(Long userId, Long articleId) {
        System.out.println("🔄 BookmarkService: Toggling bookmark - User ID: " + userId + ", Article ID: " + articleId);

        try {
            // Детальная проверка пользователя и статьи
            User user = userRepository.findById(userId).orElse(null);
            Article article = articleRepository.findById(articleId).orElse(null);

            System.out.println("👤 User found: " + (user != null ? user.getName() : "NULL"));
            System.out.println("📄 Article found: " + (article != null ? article.getTitle() : "NULL"));

            if (user == null || article == null) {
                System.out.println("❌ User or Article not found - cannot toggle bookmark");
                return false;
            }

            boolean currentlyBookmarked = bookmarkRepository.existsByUserAndArticle(user.getId(), article.getId());
            System.out.println("📊 Current bookmark status: " + currentlyBookmarked);

            if (currentlyBookmarked) {
                System.out.println("➖ Removing existing bookmark...");
                bookmarkRepository.deleteByUserAndArticle(user.getId(), article.getId());
                System.out.println("✅ Bookmark removed");
                return false;
            } else {
                System.out.println("➕ Creating new bookmark...");
                Bookmark bookmark = new Bookmark();
                bookmark.setUser(user);
                bookmark.setArticle(article);

                Bookmark savedBookmark = bookmarkRepository.save(bookmark);
                System.out.println("✅ Bookmark created with ID: " + savedBookmark.getId());
                return true;
            }
        } catch (Exception e) {
            System.err.println("❌ ERROR in toggleBookmark: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}