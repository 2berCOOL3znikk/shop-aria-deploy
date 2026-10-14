package com.aria.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.aria.enums.ProductStatus;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64, nullable = false)
    private String sku;

    @Column(length = 500, nullable = false)
    private String name;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 120)
    private String category;

    @Column(nullable = false)
    private Integer quantityOnHand = 0;

    @Column(nullable = false)
    private LocalDate receivedAt;

    @Column(nullable = false)
    private Integer salesLast30Days = 0;

    @Column(precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(precision = 12, scale = 2)
    private BigDecimal unitPrice;

    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ProductStatus status = ProductStatus.ACTIVE;

    @Column(length = 1000)
    private String ariaReason;

    private LocalDateTime lastAnalyzedAt;

    // Новое поле для средней цены рынка (добавлено один раз, без дублей)
    @Column(precision = 12, scale = 2)
    private BigDecimal marketAvgPrice;

    // Процент скидки для динамического ценообразования (0 = без скидки)
    @Column(nullable = false)
    private Integer discountPercent = 0;

    public Product() {}

    public Product(String sku, String name, String category, int qty, LocalDate receivedAt,
                   int sales30, BigDecimal cost, BigDecimal price, LocalDate expiry) {
        this.sku = sku;
        this.name = name;
        this.imageUrl = defaultImageFor(category);
        this.category = category;
        this.quantityOnHand = qty;
        this.receivedAt = receivedAt;
        this.salesLast30Days = sales30;
        this.unitCost = cost;
        this.unitPrice = price;
        this.expiryDate = expiry;
        this.status = ProductStatus.ACTIVE;
    }

    // Геттеры и сеттеры — ВСЕ поля, по одному разу
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSku() { return sku; }
    public void setSku(String v) { sku = v; }

    public String getName() { return name; }
    public void setName(String v) { name = v; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String v) { imageUrl = v; }

    public String getCategory() { return category; }
    public void setCategory(String v) { category = v; }

    public Integer getQuantityOnHand() { return quantityOnHand; }
    public void setQuantityOnHand(Integer v) { quantityOnHand = v; }

    public LocalDate getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDate v) { receivedAt = v; }

    public Integer getSalesLast30Days() { return salesLast30Days; }
    public void setSalesLast30Days(Integer v) { salesLast30Days = v; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal v) { unitCost = v; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal v) { unitPrice = v; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate v) { expiryDate = v; }

    public ProductStatus getStatus() { return status; }
    public void setStatus(ProductStatus v) { status = v; }

    public String getAriaReason() { return ariaReason; }
    public void setAriaReason(String v) { ariaReason = v; }

    public LocalDateTime getLastAnalyzedAt() { return lastAnalyzedAt; }
    public void setLastAnalyzedAt(LocalDateTime v) { lastAnalyzedAt = v; }

    public BigDecimal getMarketAvgPrice() { return marketAvgPrice; }
    public void setMarketAvgPrice(BigDecimal v) { marketAvgPrice = v; }

    public Integer getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Integer v) { discountPercent = v; }

    private static String defaultImageFor(String category) {
        if (category == null) return "/images/product-generic.svg";
        String value = category.toLowerCase(java.util.Locale.ROOT);
        if (value.contains("электрон") || value.contains("техник")) return "/images/product-electronics.svg";
        if (value.contains("дом") || value.contains("текстил")) return "/images/product-home.svg";
        if (value.contains("промо") || value.contains("подар")) return "/images/product-gifts.svg";
        if (value.contains("продукт") || value.contains("еда")) return "/images/product-food.svg";
        return "/images/product-generic.svg";
    }
}
