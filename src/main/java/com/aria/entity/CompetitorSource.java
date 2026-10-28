package com.aria.entity;
import jakarta.persistence.*;
@Entity @Table(name="competitor_sources")
public class CompetitorSource {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false,length=160) public String name;
    @Column(nullable=false,length=500) public String baseUrl;
    @Column(nullable=false) public boolean active=true;
}
