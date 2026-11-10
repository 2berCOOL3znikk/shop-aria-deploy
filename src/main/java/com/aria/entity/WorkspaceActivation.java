package com.aria.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="workspace_activation")
public class WorkspaceActivation {
 @Id public Long id=1L;
 @Column(nullable=false,unique=true,length=36) public String requestId;
 @Column(nullable=false,length=80) public String binding;
 @Column(nullable=false) public Long ownerUserId;
 @Column(nullable=false,length=64) public String ownerEmailHash;
 @Column(nullable=false,length=64) public String accessHash;
 @Column(nullable=false,length=64) public String probeHash;
 @Column(nullable=false) public Instant deadline;
 public Instant claimedAt;
 @Version public long version;
}
