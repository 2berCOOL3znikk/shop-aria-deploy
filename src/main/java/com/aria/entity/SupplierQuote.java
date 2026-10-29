package com.aria.entity;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
@Entity @Table(name="supplier_quotes",uniqueConstraints=@UniqueConstraint(columnNames={"productId","supplierId"}))
public class SupplierQuote {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Version public long version;
 @Column(nullable=false) public Long productId;
 @Column(nullable=false) public Long supplierId;
 @Column(nullable=false,precision=12,scale=2) public BigDecimal unitCost;
 @Column(nullable=false) public int minimumQuantity=1;
 @Column(nullable=false) public int leadDays;
 @Column(nullable=false) public LocalDate quotedOn;
 @Column(nullable=false) public LocalDate validUntil;
 @Column(nullable=false) public boolean verified;
 @Column(nullable=false) public LocalDateTime updatedAt=LocalDateTime.now();
}
