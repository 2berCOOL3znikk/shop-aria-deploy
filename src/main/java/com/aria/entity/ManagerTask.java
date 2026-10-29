package com.aria.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="manager_tasks",uniqueConstraints=@UniqueConstraint(columnNames="product_id"))
public class ManagerTask {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public Long productId;
 @Column(nullable=false,length=20) public String status="QUEUED";
 @Column(nullable=false) public long generation=1;
 @Column(nullable=false) public long claimedGeneration=0;
 @Column(nullable=false) public int attempts=0;
 @Column(nullable=false) public LocalDateTime dueAt=LocalDateTime.now();
 @Column(length=36) public String lease;
 public LocalDateTime leaseUntil;
 @Column(nullable=false,length=300) public String cause="INITIAL";
 @Column(length=700) public String lastError;
 @Column(nullable=false) public LocalDateTime updatedAt=LocalDateTime.now();
}
