package com.aria.entity;
import jakarta.persistence.*;
import java.time.*;
@Entity @Table(name="social_proposals",uniqueConstraints=@UniqueConstraint(name="uk_social_proposal_decision",columnNames="decisionId"))
public class SocialProposal {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Version public long version;
    @Column(nullable=false) public Long decisionId;
    @Column(nullable=false) public Long productId;
    @Column(nullable=false) public int quantity;
    @Column(nullable=false) public LocalDate validUntil;
    @Column(nullable=false,length=20) public String status="PROPOSED";
    @Column(nullable=false,length=1000) public String reason;
    @Column(nullable=false) public LocalDateTime createdAt=LocalDateTime.now();
    public Long organizationId;
    public Long offerId;
    public LocalDateTime approvedAt;
    @Column(length=255) public String approvedBy;
}
