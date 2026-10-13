package com.aria.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transfer_lots")
public class TransferLot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 120) private String partner;
    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(length = 20, nullable = false) private String state = "DRAFT";
    @Column(length = 500) private String note;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "lot_id")
    private List<Product> items = new ArrayList<>();

    public TransferLot() { this.createdAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public String getPartner() { return partner; } public void setPartner(String v){ partner=v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getState() { return state; } public void setState(String v){ state=v; }
    public String getNote() { return note; } public void setNote(String v){ note=v; }
    public List<Product> getItems() { return items; }
}