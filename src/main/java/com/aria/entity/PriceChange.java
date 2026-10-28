package com.aria.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Immutable snapshot of a committed price change; never inferred from today's Product. */
@Entity @Table(name="price_changes",indexes=@Index(name="ix_price_change_product_time",columnList="productId,changedAt"))
public class PriceChange {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false) public Long productId;
    @Column(nullable=false) public LocalDateTime changedAt;
    @Column(nullable=false,length=255) public String actor;
    @Column(nullable=false,length=1000) public String reason;
    @Column(nullable=false,precision=12,scale=2) public BigDecimal oldBasePrice;
    @Column(nullable=false,precision=12,scale=2) public BigDecimal newBasePrice;
    @Column(nullable=false) public int oldPercent;
    @Column(nullable=false) public int newPercent;
    @Column(nullable=false,precision=12,scale=2) public BigDecimal costAtChange;
    @Column(nullable=false) public int availableAtChange;
}
