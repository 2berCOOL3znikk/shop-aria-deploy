package com.aria.entity;
import jakarta.persistence.*;
@Entity @Table(name="store_snapshots",uniqueConstraints=@UniqueConstraint(columnNames={"store_id","external_id"}))
public class StoreSnapshot {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(name="store_id",nullable=false) public Long storeId;
 @Column(name="external_id",nullable=false,length=100) public String externalId;
 @Column(nullable=false,length=500) public String name;
 @Column(length=100) public String sku;
 public java.math.BigDecimal price;
 public Integer availableQuantity;
 @Column(length=20) public String currency;
 public java.time.LocalDateTime observedAt;
}
