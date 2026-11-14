package com.aria.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="saas_cloud_registrations")
public class CloudRegistration {
 @Id @Column(length=36) public String id;
 @Column(nullable=false,length=64,unique=true) public String accessHash;
 @Column(nullable=false,length=255) public String email;
 @Column(nullable=false,length=64,unique=true) public String emailHash;
 @Column(nullable=false,length=200) public String companyName;
 @Column(columnDefinition="text") public String credentialCipher;
 @Column(nullable=false,length=64) public String fingerprint;
 @Column(nullable=false,length=30) public String status;
 @Column(length=36,unique=true) public String slotId;
 @Column(nullable=false) public boolean processingConsent;
 @Column(nullable=false) public boolean authorityConfirmed;
 @Column(nullable=false,length=40) public String consentVersion="saas-registration-v1";
 @Column(nullable=false) public LocalDateTime createdAt;
 @Column(nullable=false) public LocalDateTime updatedAt;
 public LocalDateTime leaseUntil;
 @Column(nullable=false) public int attempts;
 @Column(length=40) public String failureCode;
 @Version public long version;
}
