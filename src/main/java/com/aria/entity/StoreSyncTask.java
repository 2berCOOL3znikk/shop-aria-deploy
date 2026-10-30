package com.aria.entity;
import jakarta.persistence.*;
@Entity @Table(name="store_sync_tasks",uniqueConstraints=@UniqueConstraint(columnNames={"store_id","operation"}))
public class StoreSyncTask {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(name="store_id",nullable=false) public Long storeId;
 @Column(nullable=false,length=20) public String operation;
 @Column(nullable=false,length=20) public String state;
 @Column(nullable=false) public int attempts;
 @Column(nullable=false) public long generation;
 @com.fasterxml.jackson.annotation.JsonIgnore @Column(nullable=false) public long claimedGeneration;
 @Column(nullable=false) public java.time.LocalDateTime dueAt;
 @Column(nullable=false) public java.time.LocalDateTime updatedAt;
 @Column(length=500) public String lastError;
 @com.fasterxml.jackson.annotation.JsonIgnore @Column(length=50) public String lease;
 @com.fasterxml.jackson.annotation.JsonIgnore public java.time.LocalDateTime leaseUntil;
}
