package com.aria.entity;
import jakarta.persistence.*;
@Entity @Table(name="store_pricing_policies")
public class StorePricingPolicy {
 @Id public Long storeId;
 @Version public long version;
 @Column(nullable=false,length=20) public String mode="RECOMMEND";
 @Column(nullable=false) public boolean externalAiConsent=false,scheduledAiConsent=false,priceWriteConsent=false;
 @Column(nullable=false) public int maximumDiscount=20,cooldownHours=24;
 @Column(nullable=false) public boolean allowLowConfidence=false;
 @Column(nullable=false,columnDefinition="text") public String allowedCategoriesJson="[]";
 @Column(nullable=false) public long consentRevision=0;
 @Column(nullable=false) public java.time.LocalDateTime updatedAt=java.time.LocalDateTime.now();
 @Column(nullable=false,length=255) public String updatedBy;
}
