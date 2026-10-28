package com.aria.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="smart_pricing_settings")
public class SmartPricingSettings {
    @Id public Long id=1L;
    @Version public Long version;
    @Column(nullable=false,length=20) public String mode="RECOMMEND";
    @Column(nullable=false) public int maximumDiscount=20;
    @Column(nullable=false) public int cooldownHours=24;
    @Column(nullable=false) public boolean allowLowConfidence=false;
    @Column(nullable=false) public boolean allowRestore=false;
    @Column(nullable=false,columnDefinition="text") public String allowedCategoriesJson="[]";
    @Column(nullable=false) public LocalDateTime updatedAt=LocalDateTime.now();
}
