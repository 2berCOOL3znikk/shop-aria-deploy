package com.aria.entity;
import jakarta.persistence.*;
import java.time.*;
@Entity @Table(name="manager_runs")
public class ManagerRun {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,length=36,unique=true) public String lease;
 @Column(nullable=false,length=20) public String mode;
 @Column(nullable=false,length=20) public String status="RUNNING";
 @Column(nullable=false) public LocalDateTime startedAt=LocalDateTime.now();
 public LocalDateTime finishedAt;
 @Column(nullable=false,columnDefinition="text") public String resultJson="{}";
}
