package com.aria.entity;
import jakarta.persistence.*;
@Entity @Table(name="store_price_changes",uniqueConstraints=@UniqueConstraint(columnNames={"store_id","request_key"}))
public class StorePriceChange {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public Long storeId;
 @Column(nullable=false,length=100) public String externalId;
 @Column(length=100) public String parentExternalId;
 @Column(nullable=false,length=500) public String productName;
 @Column(nullable=false,length=80) public String requestKey;
 @Version public long version;
 @Column(nullable=false) public long storeVersion,policyRevision,productPolicyVersion;
 @Column(nullable=false,precision=19,scale=2) public java.math.BigDecimal expectedPrice,targetPrice;
 @Column(nullable=false,length=3) public String currency;
 @Column(nullable=false,length=40) public String status="DRAFT";
 @Column(nullable=false,length=20) public String source="MANUAL";
 @Column(nullable=false) public boolean automatic;
 public Long decisionId;
 @Column(nullable=false,length=900) public String reason;
 @Column(length=900) public String outcomeNote;
 @Column(nullable=false,length=255) public String requestedBy;
 @Column(nullable=false) public java.time.LocalDateTime createdAt=java.time.LocalDateTime.now();
 public java.time.LocalDateTime sendingAt,completedAt;
}
