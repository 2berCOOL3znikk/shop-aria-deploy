package com.aria.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="staff_members")
public class StaffMember {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Version public long version;
 @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false,unique=true) public AppUser user;
 @Column(nullable=false) public boolean active=true;
 @Column(nullable=false) public LocalDateTime joinedAt;
 public LocalDateTime changedAt;
}
