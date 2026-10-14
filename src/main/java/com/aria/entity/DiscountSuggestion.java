package com.aria.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "discount_suggestions")
public class DiscountSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false)
    private Integer suggestedPercent;

    @Column(nullable = false)
    private LocalDateTime proposedAt;

    @Column(length = 20, nullable = false)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED

    @Column(length = 255)
    private String decidedBy;

    private LocalDateTime decidedAt;

    public DiscountSuggestion() {}

    public DiscountSuggestion(Product product, String reason, Integer suggestedPercent) {
        this.product = product;
        this.reason = reason;
        this.suggestedPercent = suggestedPercent;
        this.proposedAt = LocalDateTime.now();
        this.status = "PENDING";
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Integer getSuggestedPercent() { return suggestedPercent; }
    public void setSuggestedPercent(Integer suggestedPercent) { this.suggestedPercent = suggestedPercent; }

    public LocalDateTime getProposedAt() { return proposedAt; }
    public void setProposedAt(LocalDateTime proposedAt) { this.proposedAt = proposedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDecidedBy() { return decidedBy; }
    public void setDecidedBy(String decidedBy) { this.decidedBy = decidedBy; }

    public LocalDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }
}
