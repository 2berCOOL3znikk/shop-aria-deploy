package com.aria.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="saas_pool_slots")
public class WorkspacePoolSlot {
 @Id @Column(length=36) public String id;
 @Column(nullable=false,length=80,unique=true) public String binding;
 @Column(nullable=false,length=300,unique=true) public String origin;
 @Column(nullable=false,columnDefinition="text") public String secretCipher;
 @Column(nullable=false,length=64,unique=true) public String admissionHash;
 @Column(nullable=false,length=20) public String state;
 @Column(length=36,unique=true) public String registrationId;
 @Column(nullable=false) public LocalDateTime createdAt;
 @Version public long version;
}
