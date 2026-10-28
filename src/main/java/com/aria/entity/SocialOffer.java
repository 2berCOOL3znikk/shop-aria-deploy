package com.aria.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "social_offers")
public class SocialOffer {
    @Column(length=500) private String productNameSnapshot;
    @Column(length=64) private String skuSnapshot;
    @Column(precision=12,scale=2) private BigDecimal unitCostSnapshot;
    private LocalDate expirySnapshot;
    private LocalDateTime transferredAt;
    public String getProductNameSnapshot(){return productNameSnapshot;}
    public String getSkuSnapshot(){return skuSnapshot;}
    public BigDecimal getUnitCostSnapshot(){return unitCostSnapshot;}
    public LocalDate getExpirySnapshot(){return expirySnapshot;}
    public LocalDateTime getTransferredAt(){return transferredAt;}
    public void setTransferredAt(LocalDateTime value){transferredAt=value;}
    public void snapshotProduct(){productNameSnapshot=product.getName();skuSnapshot=product.getSku();unitCostSnapshot=product.getUnitCost();expirySnapshot=product.getExpiryDate();}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(precision = 12, scale = 2)
    private BigDecimal socialPrice;

    @Column(nullable = false)
    private Integer quantityAvailable;

    @Column(nullable = false)
    private Integer quantityClaimed = 0;

    private LocalDate validUntil;

    @Column(name = "org_id")
    private Long orgId; // ID организации-получателя (null = ещё не забрано)

    @Column(length = 20, nullable = false)
    private String status = "OPEN"; // OPEN, CLAIMED, CLOSED

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime claimedAt;

    public SocialOffer() {}

    public SocialOffer(Product product, BigDecimal socialPrice, Integer quantityAvailable, LocalDate validUntil) {
        this.product = product;
        this.socialPrice = socialPrice;
        this.quantityAvailable = quantityAvailable;
        this.validUntil = validUntil;
        this.status = "OPEN";
        this.createdAt = LocalDateTime.now();
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public BigDecimal getSocialPrice() { return socialPrice; }
    public void setSocialPrice(BigDecimal socialPrice) { this.socialPrice = socialPrice; }

    public Integer getQuantityAvailable() { return quantityAvailable; }
    public void setQuantityAvailable(Integer quantityAvailable) { this.quantityAvailable = quantityAvailable; }
    public Integer getQuantityClaimed() { return quantityClaimed; }
    public void setQuantityClaimed(Integer quantityClaimed) { this.quantityClaimed = quantityClaimed; }

    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }

    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getClaimedAt() { return claimedAt; }
    public void setClaimedAt(LocalDateTime claimedAt) { this.claimedAt = claimedAt; }
}
