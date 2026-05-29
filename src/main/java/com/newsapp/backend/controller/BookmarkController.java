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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/bookmarks")
public class BookmarkController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookmarkedArticleRepository articleRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> getBookmarks(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        User user = requireUser(userPrincipal);
        return ResponseEntity.ok(new ArrayList<>(user.getBookmarks()));
    }

    @PostMapping("/toggle")
    @Transactional
    public ResponseEntity<?> toggleBookmark(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody BookmarkRequest request) {

        if (request == null || request.getId() == null || request.getId().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Article id is required.");
        }
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            request.setTitle("Untitled");
        }

        User user = requireUser(userPrincipal);

        BookmarkedArticle article = articleRepository.findById(request.getId().trim())
                .orElseGet(() -> createArticle(request));

        final String articleId = article.getId();
        boolean alreadyBookmarked = user.getBookmarks().stream()
                .anyMatch(existing -> articleId.equals(existing.getId()));

        boolean isBookmarked;
        if (alreadyBookmarked) {
            user.getBookmarks().removeIf(existing -> articleId.equals(existing.getId()));
            isBookmarked = false;
        } else {
            user.getBookmarks().add(article);
            isBookmarked = true;
        }

        userRepository.saveAndFlush(user);

        Map<String, Object> response = new HashMap<>();
        response.put("bookmarked", isBookmarked);
        response.put("articleId", article.getId());

        return ResponseEntity.ok(response);
    }

    private User requireUser(UserPrincipal userPrincipal) {
        if (userPrincipal == null || userPrincipal.getEmail() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }
        return userRepository.findWithBookmarksByEmail(userPrincipal.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private BookmarkedArticle createArticle(BookmarkRequest request) {
        LocalDateTime publishedDate = LocalDateTime.now();
        if (request.getPublishedAt() != null && !request.getPublishedAt().isBlank()) {
            try {
                publishedDate = OffsetDateTime.parse(request.getPublishedAt()).toLocalDateTime();
            } catch (Exception e) {
                try {
                    publishedDate = LocalDateTime.parse(request.getPublishedAt(), DateTimeFormatter.ISO_DATE_TIME);
                } catch (Exception ex) {
                    publishedDate = LocalDateTime.now();
                }
            }
        }

        String imageUrl = request.getImageUrl();
        if (imageUrl != null && imageUrl.length() > 4000) {
            imageUrl = imageUrl.substring(0, 4000);
        }

        BookmarkedArticle article = BookmarkedArticle.builder()
                .id(request.getId().trim())
                .title(request.getTitle().trim())
                .content(request.getContent())
                .category(request.getCategory() != null ? request.getCategory() : "General")
                .author(request.getAuthor() != null ? request.getAuthor() : "Unknown Author")
                .imageUrl(imageUrl)
                .publishedAt(publishedDate)
                .readTimeMinutes(request.getReadTimeMinutes() != null ? request.getReadTimeMinutes() : 5)
                .build();

        return articleRepository.saveAndFlush(article);
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
