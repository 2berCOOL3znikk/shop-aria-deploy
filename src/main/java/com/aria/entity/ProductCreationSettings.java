package com.aria.entity;

import jakarta.persistence.*;
import java.time.*;

@Entity @Table(name="product_creation_settings")
public class ProductCreationSettings {
    @Id public Long id=1L;
    @Version public long version;
    @Column(nullable=false) public long consentRevision;
    @Column(nullable=false) public boolean textConsentEnabled=false;
    @Column(nullable=false) public int dailyBudget=5;
    @Column(nullable=false) public int budgetUsed;
    public LocalDate budgetDate;
    public LocalDateTime lastRequestAt;
    @Column(nullable=false) public LocalDateTime updatedAt=LocalDateTime.now(ZoneId.of("Asia/Qyzylorda"));
}
