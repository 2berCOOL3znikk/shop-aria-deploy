package com.aria.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="manager_executions")
public class ManagerExecution {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public Long taskId;
 @Column(nullable=false) public Long productId;
 @Column(nullable=false,length=36,unique=true) public String lease;
 @Column(nullable=false,length=20) public String status="RUNNING";
 @Column(nullable=false,length=30) public String source="ENGINE DECISION";
 @Column(nullable=false,length=30) public String strategy="ANALYZE";
 public Long decisionId;
 @Column(nullable=false) public boolean priceApplied=false;
 @Column(nullable=false) public boolean purchaseDraft=false;
 @Column(nullable=false) public boolean socialDraft=false;
 @Column(nullable=false,length=1000) public String summary="Задача получена исполнителем";
 @Column(nullable=false) public LocalDateTime startedAt=LocalDateTime.now();
 public LocalDateTime finishedAt;
}
