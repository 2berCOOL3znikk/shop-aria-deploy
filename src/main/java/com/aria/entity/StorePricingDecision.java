package com.aria.entity;
import jakarta.persistence.*;
@Entity @Table(name="store_pricing_decisions")
public class StorePricingDecision {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public Long storeId;
 @Column(nullable=false,length=100) public String externalId;
 @Column(nullable=false,length=500) public String productName;
 @Version public long version;
 @Column(nullable=false,length=64) public String fingerprint;
 @com.fasterxml.jackson.annotation.JsonIgnore @Column(nullable=false,columnDefinition="text") public String reportJson;
 @com.fasterxml.jackson.annotation.JsonIgnore @Column(columnDefinition="text") public String explanationJson;
 @Column(nullable=false,length=20) public String source="ENGINE",strategy="KEEP",confidence="LOW";
 @Column(nullable=false,length=30) public String status="PENDING";
 @Column(precision=19,scale=2) public java.math.BigDecimal currentPrice,suggestedPrice;
 @Column(length=3) public String currency;
 @Column(nullable=false) public int selectedPercent;
 @Column(nullable=false) public boolean confirmationRequired=true;
 @Column(nullable=false,length=2000) public String reason;
 @Column(nullable=false) public java.time.LocalDateTime createdAt=java.time.LocalDateTime.now(),reviewAt;
 public java.time.LocalDateTime aiRequestedAt,aiCompletedAt;
 @com.fasterxml.jackson.annotation.JsonIgnore @Column(length=80) public String aiClaim;
 public Long globalConsentRevision,storeConsentRevision,priceChangeId;
}
