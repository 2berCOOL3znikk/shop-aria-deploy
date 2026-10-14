package com.aria.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cart_items")
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal priceAtAddTime;

    @Column(nullable = false)
    private LocalDateTime addedAt;

    @Column(length = 255)
    private String ownerEmail;

    public CartItem() {
        this.addedAt = LocalDateTime.now();
    }

    public CartItem(Product product, int qty, BigDecimal price) {
        this.product = product;
        this.quantity = qty;
        this.priceAtAddTime = price;
        this.addedAt = LocalDateTime.now();
    }

    public CartItem(Product product, int qty, BigDecimal price, String ownerEmail) {
        this(product, qty, price);
        this.ownerEmail = ownerEmail;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Product getProduct() { return product; }
    public void setProduct(Product p) { this.product = p; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer q) { this.quantity = q; }
    public BigDecimal getPriceAtAddTime() { return priceAtAddTime; }
    public void setPriceAtAddTime(BigDecimal p) { this.priceAtAddTime = p; }
    public LocalDateTime getAddedAt() { return addedAt; }
    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }
}
