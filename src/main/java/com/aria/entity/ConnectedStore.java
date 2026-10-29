package com.aria.entity;

import jakarta.persistence.*;

@Entity @Table(name="connected_stores")
public class ConnectedStore {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(optional=false) @JoinColumn(name="organization_id",nullable=false) public Organization organization;
 @Column(nullable=false,length=200) public String name;
 @Column(nullable=false,length=30) public String platform;
 @Column(name="base_url",nullable=false,length=500) public String baseUrl;
 @Column(nullable=false,length=30) public String status="NOT_CONNECTED";
 @Column(name="write_enabled",nullable=false) public boolean writeEnabled=false;
 @Version public long version;
 public java.time.LocalDateTime lastSync;
 @Column(length=500) public String lastError;
}
