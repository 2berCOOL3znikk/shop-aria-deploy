package com.aria.entity;
import jakarta.persistence.*;
@Entity @Table(name="charity_acceptance")
public class CharityAcceptance {
    @Id public Long organizationId;
    @Column(nullable=false,length=120) public String region;
    @Column(nullable=false,columnDefinition="text") public String categoriesJson;
    @Column(nullable=false,length=1000) public String storageConditions;
    @Column(nullable=false) public int minimumRemainingDays;
    @Column(nullable=false) public boolean active=true;
}
