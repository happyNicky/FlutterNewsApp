package com.newsapp.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "bookmarked_articles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookmarkedArticle {
    @Id
    private String id; // We will use the URL hash or original ID as the primary key

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String category;

    private String author;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    private LocalDateTime publishedAt;

    private Integer readTimeMinutes;

    @ManyToMany(mappedBy = "bookmarks", fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private Set<User> users = new HashSet<>();
}
