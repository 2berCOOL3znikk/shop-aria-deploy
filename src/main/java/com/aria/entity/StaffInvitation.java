package com.aria.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="staff_invitations")
public class StaffInvitation {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Version public long version;
 @Column(nullable=false,length=64,unique=true) public String tokenHash;
 @Column(nullable=false,length=255) public String email;
 @Column(nullable=false,length=24) public String status="PENDING";
 @Column(nullable=false) public LocalDateTime createdAt;
 @Column(nullable=false) public LocalDateTime expiresAt;
 @Column(nullable=false,length=255) public String createdBy;
 public Long acceptedUserId;
}
