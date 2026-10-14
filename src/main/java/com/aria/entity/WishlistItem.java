package com.aria.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "wishlist_items")
public class WishlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private LocalDateTime addedAt;

    @Column(length = 255)
    private String ownerEmail;

    public WishlistItem() {
        this.addedAt = LocalDateTime.now();
    }

    public WishlistItem(Product product) {
        this.product = product;
        this.addedAt = LocalDateTime.now();
    }

    public WishlistItem(Product product, String ownerEmail) {
        this(product);
        this.ownerEmail = ownerEmail;
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public void setProduct(Product p) { this.product = p; }
    public LocalDateTime getAddedAt() { return addedAt; }
    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }
}
