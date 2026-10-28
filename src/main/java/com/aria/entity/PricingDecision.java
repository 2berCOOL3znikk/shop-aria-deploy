package com.aria.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name="pricing_decisions",indexes=@Index(name="ix_pricing_decision_product_time",columnList="productId,createdAt"))
public class PricingDecision {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Version public long version;
    @Column(nullable=false) public Long productId;
    @Column(nullable=false) public LocalDateTime createdAt;
    @Column(nullable=false) public LocalDateTime reviewAt;
    @Column(nullable=false,length=20) public String status="PENDING";
    @Column(nullable=false,length=64) public String fingerprint;
    @Column(nullable=false,columnDefinition="text") public String reportJson;
    @Column(columnDefinition="text") public String explanationJson;
    @Column(nullable=false,length=10,columnDefinition="varchar(10) default 'ENGINE'") public String source="ENGINE";
    @Column(nullable=false,length=20,columnDefinition="varchar(20) default 'PRICE'") public String strategy="PRICE";
    public Integer selectedPercent;
    public LocalDateTime aiRequestedAt;
    public LocalDateTime aiCompletedAt;
    @Column(length=36) public String aiClaim;
    public LocalDateTime decidedAt;
    @Column(length=255) public String decidedBy;
    @Column(length=1000) public String decisionReason;
    public Long priceChangeId;
    public Integer appliedPercent;
    @Column(precision=12,scale=2) public BigDecimal appliedPrice;
}
