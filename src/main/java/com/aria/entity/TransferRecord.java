package com.aria.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfer_records", uniqueConstraints = @UniqueConstraint(name = "uk_transfer_record_lot_product", columnNames = {"lot_id", "product_id"}))
public class TransferRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "lot_id", nullable = false) private Long lotId;
    @Column(name = "product_id", nullable = false) private Long productId;
    @Column(nullable = false, length = 64) private String sku;
    @Column(nullable = false, length = 500) private String productName;
    @Column(nullable = false) private Integer quantity;
    @Column(precision = 12, scale = 2) private BigDecimal unitCost;
    @Column(nullable = false, length = 120) private String partner;
    @Column(nullable = false) private LocalDateTime transferredAt;

    public TransferRecord() {}
    public TransferRecord(Long lotId, Long productId, String sku, String productName, Integer quantity, BigDecimal unitCost, String partner, LocalDateTime transferredAt) {
        this.lotId = lotId; this.productId = productId; this.sku = sku; this.productName = productName;
        this.quantity = quantity; this.unitCost = unitCost; this.partner = partner; this.transferredAt = transferredAt;
    }
    public Long getId() { return id; }
    public Long getLotId() { return lotId; }
    public Long getProductId() { return productId; }
    public String getSku() { return sku; }
    public String getProductName() { return productName; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getUnitCost() { return unitCost; }
    public String getPartner() { return partner; }
    public LocalDateTime getTransferredAt() { return transferredAt; }
}