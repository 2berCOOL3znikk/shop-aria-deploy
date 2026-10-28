package com.aria.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;

@Entity
@Table(name="audit_events")
public class AuditEvent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false)
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(nullable=false,length=255)
    private String actor;
    @Column(nullable=false,length=80)
    private String action;
    @Column(nullable=false,length=120)
    private String resource;
    @Column(nullable=false,length=2000)
    private String details;
    public AuditEvent() {}
    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt = value; }
    public String getActor() { return actor; }
    public void setActor(String value) { actor = value; }
    public String getAction() { return action; }
    public void setAction(String value) { action = value; }
    public String getResource() { return resource; }
    public void setResource(String value) { resource = value; }
    public String getDetails() { return details; }
    public void setDetails(String value) { details = value; }
}
