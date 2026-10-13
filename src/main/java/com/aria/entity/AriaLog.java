package com.aria.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "aria_logs")
public class AriaLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime analyzedAt;

    @Column(nullable = false)
    private Integer candidatesFound;

    @Column(nullable = false)
    private Integer returnedToActive;

    @Column(nullable = false)
    private Integer urgentItems;

    @Column(nullable = false)
    private Integer totalScanned;

    @Column(length = 1000)
    private String details;

    public AriaLog() {
        this.analyzedAt = LocalDateTime.now();
    }

    public AriaLog(LocalDateTime analyzedAt, Integer candidatesFound, Integer returnedToActive,
                   Integer urgentItems, Integer totalScanned, String details) {
        this.analyzedAt = analyzedAt;
        this.candidatesFound = candidatesFound;
        this.returnedToActive = returnedToActive;
        this.urgentItems = urgentItems;
        this.totalScanned = totalScanned;
        this.details = details;
    }

    public Long getId() { return id; }
    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public Integer getCandidatesFound() { return candidatesFound; }
    public Integer getReturnedToActive() { return returnedToActive; }
    public Integer getUrgentItems() { return urgentItems; }
    public Integer getTotalScanned() { return totalScanned; }
    public String getDetails() { return details; }
}