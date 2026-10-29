package com.aria.entity;
import jakarta.persistence.*;
import java.time.*;
@Entity @Table(name="pos_receipts")
public class PosReceipt {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Version public long version;
 @Column(nullable=false,length=80,unique=true) public String requestKey;
 @Column(nullable=false,length=64) public String fingerprint;
 @Column(nullable=false,unique=true) public Long orderId;
 @Column(nullable=false,length=255) public String cashier;
 @Column(nullable=false,length=20) public String status="SALE";
 @Column(nullable=false) public LocalDateTime createdAt=LocalDateTime.now();
 public LocalDateTime returnedAt;
 @Column(length=900) public String returnReason;
}
