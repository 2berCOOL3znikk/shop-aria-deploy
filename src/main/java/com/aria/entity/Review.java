package com.aria.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(length = 100, nullable = false)
    private String authorName;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String comment;

    @Column(nullable = false)
    private Integer rating;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Review() {
        this.createdAt = LocalDateTime.now();
    }

    public Review(Product product, String author, String comment, int rating) {
        this.product = product;
        this.authorName = author;
        this.comment = comment;
        this.rating = Math.max(1, Math.min(5, rating));
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public void setProduct(Product p) { this.product = p; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String a) { this.authorName = a; }
    public String getComment() { return comment; }
    public void setComment(String c) { this.comment = c; }
    public Integer getRating() { return rating; }
    public void setRating(Integer r) { this.rating = r; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}