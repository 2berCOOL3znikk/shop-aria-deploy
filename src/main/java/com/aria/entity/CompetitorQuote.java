package com.aria.entity;
import jakarta.persistence.*;
import java.time.Instant;
import java.math.BigDecimal;
@Entity @Table(name="competitor_quotes",uniqueConstraints=@UniqueConstraint(name="uk_competitor_quote",columnNames={"sourceId","productId","observedAt"}))
public class CompetitorQuote {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false) public Long sourceId;
    @Column(nullable=false) public Long productId;
    @Column(nullable=false,length=64) public String barcode;
    @Column(nullable=false,length=20) public String unit;
    @Column(nullable=false,precision=12,scale=2) public BigDecimal price;
    @Column(nullable=false,length=3) public String currency="KZT";
    @Column(nullable=false) public Instant observedAt;
    @Column(nullable=false,length=1000) public String url;
    @Column(nullable=false) public Instant importedAt=Instant.now();
}
