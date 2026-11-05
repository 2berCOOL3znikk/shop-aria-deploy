package com.aria.entity;
import jakarta.persistence.*;
@Entity @Table(name="store_product_policies",uniqueConstraints=@UniqueConstraint(columnNames={"store_id","external_id"}))
public class StoreProductPolicy {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public Long storeId;
 @Column(nullable=false,length=100) public String externalId;
 @Version public long version;
 @Column(nullable=false,precision=19,scale=2) public java.math.BigDecimal unitCost,minimumPrice;
 @Column(precision=19,scale=2) public java.math.BigDecimal referencePrice;
 @Column(length=3) public String currency;
 @Column(nullable=false,length=200) public String category;
 @Column(nullable=false) public boolean nonPerishableConfirmed;
 @Column(nullable=false) public java.time.LocalDateTime confirmedAt;
 @Column(nullable=false,length=255) public String confirmedBy;
 @Column(nullable=false,length=900) public String reason;
}
