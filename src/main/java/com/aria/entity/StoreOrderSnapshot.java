package com.aria.entity;
import jakarta.persistence.*;
@Entity @Table(name="store_order_snapshots",uniqueConstraints=@UniqueConstraint(columnNames={"store_id","external_id"}))
public class StoreOrderSnapshot {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(name="store_id",nullable=false) public Long storeId;
 @Column(name="external_id",nullable=false,length=100) public String externalId;
 @Column(nullable=false,length=40) public String status;
 @Column(nullable=false,length=3) public String currency;
 @Column(nullable=false,precision=38,scale=6) public java.math.BigDecimal total;
 @Column(precision=38,scale=6) public java.math.BigDecimal refundedTotal;
 public java.time.LocalDateTime createdAtUtc;
 @Column(nullable=false) public java.time.LocalDateTime modifiedAtUtc;
 public java.time.LocalDateTime completedAtUtc;
 @Column(nullable=false) public java.time.LocalDateTime observedAt;
 @Column(nullable=false,columnDefinition="text") @com.fasterxml.jackson.annotation.JsonIgnore public String linesJson;
}
