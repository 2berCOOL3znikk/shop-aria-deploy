package com.aria.repository;
import com.aria.entity.PricingDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import java.util.*;
public interface PricingDecisionRepository extends JpaRepository<PricingDecision,Long> {
    Optional<PricingDecision> findFirstByProductIdOrderByCreatedAtDescIdDesc(Long productId);
    List<PricingDecision> findByProductIdAndStatus(Long productId,String status);
    Page<PricingDecision> findAllByOrderByCreatedAtDescIdDesc(Pageable page);
    @org.springframework.data.jpa.repository.Query("select d.id from PricingDecision d where d.status='PENDING' and d.reviewAt>:now and (d.aiCompletedAt is null or d.aiCompletedAt<:retryBefore) and (d.aiRequestedAt is null or d.aiRequestedAt<:claimBefore) order by d.reviewAt,d.id")
    List<Long> aiCandidates(@org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now,@org.springframework.data.repository.query.Param("retryBefore") java.time.LocalDateTime retryBefore,@org.springframework.data.repository.query.Param("claimBefore") java.time.LocalDateTime claimBefore,Pageable page);
}
