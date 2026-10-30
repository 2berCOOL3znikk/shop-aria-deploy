package com.aria.entity;
import jakarta.persistence.*;
@Entity @Table(name="store_webhook_receipts",uniqueConstraints=@UniqueConstraint(columnNames={"store_id","generation","resource","body_hash"}))
public class StoreWebhookReceipt {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(name="store_id",nullable=false) public Long storeId;
 @Column(nullable=false,length=64) public String generation;
 @Column(nullable=false,length=20) public String resource;
 @Column(name="body_hash",nullable=false,length=64) public String bodyHash;
 public Long taskId;
 @Column(nullable=false,length=20) public String status;
 @Column(nullable=false) public int bodyBytes;
 @Column(nullable=false) public java.time.LocalDateTime receivedAt;
}
