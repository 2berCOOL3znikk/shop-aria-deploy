package com.aria.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="company_registration_requests")
public class CompanyRegistrationRequest {
 @Id @Column(length=36) public String id;
 @Column(nullable=false,length=200) public String companyName;
 @Column(nullable=false,length=120) public String ownerName;
 @Column(nullable=false,length=255) public String ownerEmail;
 @Column(nullable=false,unique=true,length=64) public String accessHash;
 @Column(nullable=false,length=30) public String status="SUBMITTED";
 @Column(nullable=false,length=40) public String consentVersion="company-request-v1";
 @Column(nullable=false) public boolean processingConsent;
 @Column(nullable=false) public boolean contactConsent;
 @Column(nullable=false) public LocalDateTime createdAt;
 @Column(nullable=false) public LocalDateTime updatedAt;
 public LocalDateTime reviewedAt;
 @Column(length=255) public String reviewedBy;
 @Column(length=700) public String reviewNote;
 @Column(length=80,unique=true) public String workspaceBinding;
 @Column(length=500) public String workspaceUrl;
 @Column(columnDefinition="text") public String setupSecretCipher;
 public java.time.Instant activationDeadline;
 public java.time.Instant workspaceCheckedAt;
 public java.time.Instant workspaceActivatedAt;
 @Version public long version;
}
