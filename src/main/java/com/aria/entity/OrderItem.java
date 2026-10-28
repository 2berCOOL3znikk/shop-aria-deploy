package com.aria.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(length=500)
    private String productName;
    @Column(length=64)
    private String productSku;
    public String getProductName(){return productName;}
    public String getProductSku(){return productSku;}

    @Column(nullable = false)
    private Integer quantity;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal pricePerUnit;

    public OrderItem() {}

    public OrderItem(Product product, int qty, BigDecimal price) {
        this.product = product;
        this.productName=product.getName();
        this.productSku=product.getSku();
        this.quantity = qty;
        this.pricePerUnit = price;
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public void setProduct(Product p) { this.product = p; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer q) { this.quantity = q; }
    public BigDecimal getPricePerUnit() { return pricePerUnit; }
    public void setPricePerUnit(BigDecimal p) { this.pricePerUnit = p; }
}
