package com.aria.entity;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
@Entity @Table(name="ai_manager_state")
public class AiManagerState {
 @Id public Long id=1L;
 @Version public long version;
 @Column(nullable=false,length=20) public String mode="ADVISOR";
 @Column(nullable=false) public long configRevision=0;
 @Column(nullable=false) public boolean automaticAiConsent=false;
 @Column(nullable=false) public boolean socialDrafts=true;
 @Column(nullable=false) public boolean purchaseDrafts=false;
 @Column(nullable=false) public int intervalMinutes=30;
 @Column(nullable=false,precision=12,scale=2) public BigDecimal dailyDraftBudget=BigDecimal.ZERO;
 @Column(length=36) public String lease;
 public LocalDateTime leaseUntil;
 public LocalDateTime nextRun;
 public LocalDateTime dirtyAt;
 public LocalDateTime lastStarted;
 public LocalDateTime lastFinished;
 @Column(nullable=false) public long productCursor=0;
 @Column(length=1000) public String lastError;
}
