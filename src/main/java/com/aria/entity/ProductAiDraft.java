package com.aria.entity;

import jakarta.persistence.*;
import java.time.*;

/** Generated copy is a suggestion, never evidence about product composition or a lot. */
@Entity @Table(name="product_ai_drafts")
public class ProductAiDraft {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Version public long version;
    @Column(nullable=false,length=20) public String status="REQUESTED";
    @Column(nullable=false) public long consentRevision;
    @Column(nullable=false,length=300) public String inputName;
    @Column(nullable=false,length=20) public String source="AI";
    @Column(length=200) public String model;
    @Column(length=300) public String name;
    @Column(length=120) public String category;
    @Column(length=1200) public String description;
    @Column(columnDefinition="text") public String questionsJson;
    @Column(length=300) public String error;
    @Column(nullable=false) public LocalDateTime createdAt=LocalDateTime.now(ZoneId.of("Asia/Qyzylorda"));
    public LocalDateTime completedAt;
    public LocalDateTime confirmedAt;
    @Column(unique=true) public Long productId;
}
