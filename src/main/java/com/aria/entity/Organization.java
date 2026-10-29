package com.aria.entity;

import jakarta.persistence.*;

/** Single organization bound to this isolated database. Not a charity recipient. */
@Entity @Table(name="saas_organization")
public class Organization {
 @Id public Integer id=1;
 @Column(name="organization_key",nullable=false,unique=true,length=80) public String organizationKey;
 @Column(nullable=false,length=200) public String name;
 @Column(nullable=false) public boolean demonstration;
 @Column(name="impact_enabled",nullable=false) public boolean impactEnabled=false;
}
