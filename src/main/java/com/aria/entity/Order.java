package com.aria.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime shippedAt;
    public LocalDateTime getShippedAt(){return shippedAt;}
    public void setShippedAt(LocalDateTime value){shippedAt=value;}

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(length = 20, nullable = false)
    private String status = "PENDING"; // PENDING -> SHIPPED -> DELIVERED -> CANCELLED

    @Column(length = 500)
    private String deliveryAddress;

    @Column(length = 255)
    private String ownerEmail;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id")
    private List<OrderItem> items = new ArrayList<>();

    public Order() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal t) { this.totalAmount = t; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String a) { this.deliveryAddress = a; }
    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }
    public List<OrderItem> getItems() { return items; }
}
