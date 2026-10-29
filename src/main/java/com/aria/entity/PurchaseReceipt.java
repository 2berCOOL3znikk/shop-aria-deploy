package com.aria.entity;
import jakarta.persistence.*;
import java.time.*;
@Entity @Table(name="purchase_receipts")
public class PurchaseReceipt {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public Long requestId;
 @Column(nullable=false) public Long lotId;
 @Column(nullable=false,length=80,unique=true) public String requestKey;
 @Column(nullable=false,length=64) public String fingerprint;
 @Column(nullable=false) public int quantity;
 @Column(nullable=false) public LocalDateTime receivedAt=LocalDateTime.now();
}
