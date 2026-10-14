package com.aria.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "feedback")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 2000, nullable = false)
    private String message;

    @Column(length = 255)
    private String email;

    @Column(nullable = false)
    private Integer rating; // 1-5

    @Column(nullable = false)
    private java.time.LocalDateTime createdAt;

    @Column(nullable = false)
    private Boolean resolved = false;

    public Feedback() {}

    public Feedback(String message, String email, Integer rating) {
        this.message = message;
        this.email = email;
        this.rating = rating;
        this.createdAt = java.time.LocalDateTime.now();
        this.resolved = false;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Boolean getResolved() { return resolved; }
    public void setResolved(Boolean resolved) { this.resolved = resolved; }
}
