package com.newsapp.backend.controller;

import com.newsapp.backend.model.BookmarkedArticle;
import com.newsapp.backend.model.User;
import com.newsapp.backend.repository.BookmarkedArticleRepository;
import com.newsapp.backend.repository.UserRepository;
import com.newsapp.backend.security.UserPrincipal;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/bookmarks")
public class BookmarkController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookmarkedArticleRepository articleRepository;

    @GetMapping
    public ResponseEntity<Set<BookmarkedArticle>> getBookmarks(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        User user = userRepository.findByEmail(userPrincipal.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(user.getBookmarks());
    }

    @PostMapping("/toggle")
    @Transactional
    public ResponseEntity<?> toggleBookmark(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody BookmarkRequest request) {

        User user = userRepository.findByEmail(userPrincipal.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<BookmarkedArticle> articleOptional = articleRepository.findById(request.getId());
        BookmarkedArticle article;

        if (articleOptional.isPresent()) {
            article = articleOptional.get();
        } else {
            LocalDateTime publishedDate = null;
            if (request.getPublishedAt() != null) {
                try {
                    // Try parsing standard ISO string
                    publishedDate = LocalDateTime.parse(request.getPublishedAt(), DateTimeFormatter.ISO_DATE_TIME);
                } catch (Exception e) {
                    publishedDate = LocalDateTime.now();
                }
            } else {
                publishedDate = LocalDateTime.now();
            }

            article = BookmarkedArticle.builder()
                    .id(request.getId())
                    .title(request.getTitle())
                    .content(request.getContent())
                    .category(request.getCategory())
                    .author(request.getAuthor())
                    .imageUrl(request.getImageUrl())
                    .publishedAt(publishedDate)
                    .readTimeMinutes(request.getReadTimeMinutes())
                    .build();
            article = articleRepository.save(article);
        }

        boolean isBookmarked;
        if (user.getBookmarks().contains(article)) {
            user.getBookmarks().remove(article);
            isBookmarked = false;
        } else {
            user.getBookmarks().add(article);
            isBookmarked = true;
        }

        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("bookmarked", isBookmarked);
        response.put("articleId", article.getId());

        return ResponseEntity.ok(response);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookmarkRequest {
        private String id;
        private String title;
        private String content;
        private String category;
        private String author;
        private String imageUrl;
        private String publishedAt;
        private Integer readTimeMinutes;
    }
}
