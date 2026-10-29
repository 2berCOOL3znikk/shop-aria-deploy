package com.aria.entity;
import jakarta.persistence.*;
import java.time.*;
@Entity @Table(name="manager_actions",uniqueConstraints=@UniqueConstraint(columnNames="signalKey"))
public class ManagerAction {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Version public long version;
 @Column(nullable=false,length=120) public String signalKey;
 public Long productId;
 public Long decisionId;
 @Column(nullable=false,length=40) public String type;
 @Column(nullable=false,length=20) public String severity;
 @Column(nullable=false,length=20) public String status="OPEN";
 @Column(nullable=false,length=500) public String title;
 @Column(nullable=false,columnDefinition="text") public String reason;
 @Column(nullable=false,columnDefinition="text") public String factsJson="{}";
 @Column(nullable=false,length=64) public String fingerprint;
 @Column(nullable=false) public LocalDateTime firstSeen=LocalDateTime.now();
 @Column(nullable=false) public LocalDateTime lastSeen=LocalDateTime.now();
 public LocalDateTime resolvedAt;
}
