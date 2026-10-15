package com.aria.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "organization_verifications", uniqueConstraints = @UniqueConstraint(name = "uk_org_verification_user", columnNames = "user_id"))
public class OrganizationVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, length = 160)
    private String organizationName;

    @Column(nullable = false, length = 100)
    private String registrationNumber;

    @Column(nullable = false, length = 32)
    private String status = "PENDING_VERIFICATION";

    @Column(nullable = false)
    private LocalDateTime submittedAt = LocalDateTime.now();

    private LocalDateTime reviewedAt;

    @Column(length = 255)
    private String reviewerEmail;

    @Column(length = 500)
    private String reviewNote;

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long value) { userId = value; }
    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String value) { organizationName = value; }
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String value) { registrationNumber = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime value) { submittedAt = value; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime value) { reviewedAt = value; }
    public String getReviewerEmail() { return reviewerEmail; }
    public void setReviewerEmail(String value) { reviewerEmail = value; }
    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String value) { reviewNote = value; }
}