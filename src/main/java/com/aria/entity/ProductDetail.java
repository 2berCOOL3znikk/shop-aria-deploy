package com.aria.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "product_details")
public class ProductDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(length = 100)
    private String brand;

    @Column(length = 50)
    private String color;

    @Column(length = 50)
    private String size;

    @Column(precision = 8, scale = 2)
    private BigDecimal weightKg;

    @Column(columnDefinition = "TEXT")
    private String descriptionLong;

    public ProductDetail() {}

    public ProductDetail(Product product, String brand, String color, String size,
                         BigDecimal weightKg, String desc) {
        this.product = product;
        this.brand = brand;
        this.color = color;
        this.size = size;
        this.weightKg = weightKg;
        this.descriptionLong = desc;
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public void setProduct(Product p) { this.product = p; }
    public String getBrand() { return brand; }
    public void setBrand(String b) { this.brand = b; }
    public String getColor() { return color; }
    public void setColor(String c) { this.color = c; }
    public String getSize() { return size; }
    public void setSize(String s) { this.size = s; }
    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal w) { this.weightKg = w; }
    public String getDescriptionLong() { return descriptionLong; }
    public void setDescriptionLong(String d) { this.descriptionLong = d; }
}