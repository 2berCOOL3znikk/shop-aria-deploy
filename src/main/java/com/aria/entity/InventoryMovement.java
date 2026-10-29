package com.aria.entity;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
@Entity @Table(name="inventory_movements")
public class InventoryMovement {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public Long productId;
 @Column(nullable=false) public Long lotId;
 @Column(nullable=false) public int quantityDelta;
 @Column(nullable=false) public int quarantineUnits=0;
 @Column(precision=12,scale=2) public BigDecimal unitCost;
 @Column(nullable=false,length=40) public String type;
 @Column(nullable=false,length=1000) public String reason;
 @Column(nullable=false,length=255) public String actor;
 @Column(nullable=false) public LocalDateTime createdAt=LocalDateTime.now();
}
