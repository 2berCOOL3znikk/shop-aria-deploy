package com.aria.entity;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
@Entity @Table(name="purchase_requests")
public class PurchaseRequest {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Version public long version;
 @Column(nullable=false) public Long productId;
 @Column(nullable=false) public Long supplierId;
 @Column(nullable=false) public Long quoteId;
 @Column(nullable=false) public int quantity;
 @Column(nullable=false) public int receivedQuantity=0;
 @Column(nullable=false,precision=12,scale=2) public BigDecimal unitCost;
 @Column(nullable=false,precision=12,scale=2) public BigDecimal estimatedTotal;
 @Column(nullable=false) public boolean managerCreated=false;
 @Column(nullable=false,length=20) public String status="DRAFT";
 @Column(nullable=false,length=500) public String productName;
 @Column(nullable=false,length=160) public String supplierName;
 @Column(nullable=false,length=1000) public String reason;
 @Column(nullable=false) public LocalDateTime createdAt=LocalDateTime.now();
 public LocalDateTime approvedAt;
 public LocalDateTime completedAt;
}
