package com.aria.entity;
import jakarta.persistence.*;
@Entity @Table(name="store_sync_runs")
public class StoreSyncRun {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public Long storeId;
 @Column(nullable=false,length=20) public String operation;
 @Column(nullable=false,length=20) public String status;
 @Column(nullable=false) public int imported;
 @Column(nullable=false) public int skipped;
 @Column(length=500) public String note;
 @Column(nullable=false) public java.time.LocalDateTime completedAt;
}
