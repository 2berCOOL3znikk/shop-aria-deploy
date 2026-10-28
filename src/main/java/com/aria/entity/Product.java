package com.aria.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.aria.enums.ProductStatus;

@Entity
@Table(name = "products", uniqueConstraints=@UniqueConstraint(name="uk_product_sku",columnNames="sku"))
public class Product {
    @Column(nullable=false, columnDefinition="boolean default false")
    private boolean archived = false;
    @Version @Column(nullable=false,columnDefinition="bigint default 0")
    private long version;
    public boolean getArchived() { return archived; }
    public void setArchived(boolean value) { archived=value; }

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

    @Column(precision=12,scale=2)
    private BigDecimal minimumPrice;
    public BigDecimal getMinimumPrice(){return minimumPrice;}
    public void setMinimumPrice(BigDecimal value){minimumPrice=value;}

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

    // === Поля для админ-панели (все nullable, backward compatible) ===
    @Column(length = 64)
    private String barcode;              // Штрихкод EAN/UPC

    @Column
    private Integer vatPercent = 20;     // НДС (%)

    @Column(length = 20)
    private String unit = "шт";          // Единица измерения (шт/кг/л)

    @Column(length = 120)
    private String subcategory;           // Подкатегория

    @Column(length = 500)
    private String tags;                // Теги (через запятую)

    @Column(length = 120)
    private String supplier;           // Поставщик

    @Column(length = 2000)
    private String notes;               // Примечания

    @Column(length = 100)
    private String dimensions;          // Габариты (Д×Ш×В)

    @Column
    private Integer lowStockThreshold = 5; // Минимальный остаток для уведомлений

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

    // === Геттеры и сеттеры новых полей ===
    public String getBarcode() { return barcode; }
    public void setBarcode(String v) { barcode = v; }

    public Integer getVatPercent() { return vatPercent; }
    public void setVatPercent(Integer v) { vatPercent = v; }

    public String getUnit() { return unit; }
    public void setUnit(String v) { unit = v; }

    public String getSubcategory() { return subcategory; }
    public void setSubcategory(String v) { subcategory = v; }

    public String getTags() { return tags; }
    public void setTags(String v) { tags = v; }

    public String getSupplier() { return supplier; }
    public void setSupplier(String v) { supplier = v; }

    public String getNotes() { return notes; }
    public void setNotes(String v) { notes = v; }

    public String getDimensions() { return dimensions; }
    public void setDimensions(String v) { dimensions = v; }

    public Integer getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(Integer v) { lowStockThreshold = v; }

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
