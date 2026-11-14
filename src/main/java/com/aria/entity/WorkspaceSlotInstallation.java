package com.aria.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="saas_slot_installation")
public class WorkspaceSlotInstallation {
 @Id public Integer id=1;
 @Column(nullable=false,length=80) public String binding;
 @Column(length=36) public String registrationId;
 @Column(length=64) public String emailHash;
 @Column(length=64) public String companyHash;
 @Column(length=64) public String credentialHash;
 public Long ownerUserId;
 public LocalDateTime installedAt;
 @Version public long version;
}
