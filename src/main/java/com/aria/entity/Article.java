package com.aria.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "articles")
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500)   private String title;
    @Column(length = 1000)  private String url;
    @Column(length = 10000) private String content;
    @Column(length = 1000)  private String description;
    @Column(length = 2000)  private String imageUrl;
    @Column(length = 20)    private String language;

    @Column(nullable = false)
    private Integer wordCount = 0;   // Integer, не int: иначе NOT NULL-колонка падает на старых строках

    @Column(nullable = false)
    private LocalDateTime parsedAt;

    public Article() {
        this.parsedAt = LocalDateTime.now();
        this.wordCount = 0;
    }

    public Article(String title, String url, String content) {
        this.title = title;
        this.url = url;
        this.content = content;
        this.parsedAt = LocalDateTime.now();
        this.wordCount = (content == null) ? 0 : content.split("\\s+").length;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public Integer getWordCount() { return wordCount; }
    public void setWordCount(Integer wordCount) { this.wordCount = wordCount; }
    public LocalDateTime getParsedAt() { return parsedAt; }
    public void setParsedAt(LocalDateTime parsedAt) { this.parsedAt = parsedAt; }
}