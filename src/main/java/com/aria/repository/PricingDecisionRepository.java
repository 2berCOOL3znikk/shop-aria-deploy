package com.aria.repository;
import com.aria.entity.PricingDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import java.util.*;
public interface PricingDecisionRepository extends JpaRepository<PricingDecision,Long> {
    Optional<PricingDecision> findFirstByProductIdOrderByCreatedAtDescIdDesc(Long productId);
    List<PricingDecision> findByProductIdAndStatus(Long productId,String status);
    Page<PricingDecision> findAllByOrderByCreatedAtDescIdDesc(Pageable page);
    @org.springframework.data.jpa.repository.Query("select d from PricingDecision d where exists (select p.id from Product p where p.id=d.productId and p.archived=false) and not exists (select newer.id from PricingDecision newer where newer.productId=d.productId and (newer.createdAt>d.createdAt or (newer.createdAt=d.createdAt and newer.id>d.id))) order by d.createdAt desc,d.id desc")
    @org.springframework.data.jpa.repository.QueryHints(@jakarta.persistence.QueryHint(name="org.hibernate.fetchSize",value="50"))
    java.util.stream.Stream<PricingDecision> streamCurrentWorkspace();
    /** A model can complete an older recommendation; its latest activity must remain visible. */
    @org.springframework.data.jpa.repository.Query("select d from PricingDecision d order by case when coalesce(d.aiCompletedAt,d.createdAt)>d.createdAt then d.aiCompletedAt else d.createdAt end desc,d.id desc")
    List<PricingDecision> findRecentActivity(Pageable page);
    @org.springframework.data.jpa.repository.Query("select d.id from PricingDecision d where d.status='PENDING' and d.reviewAt>:now and (d.aiLastSkippedAt is null or d.aiLastSkippedAt<:retryBefore) and (d.aiCompletedAt is null or d.aiCompletedAt<:retryBefore) and (d.aiRequestedAt is null or d.aiRequestedAt<:claimBefore) order by d.reviewAt,d.id")
    List<Long> aiCandidates(@org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now,@org.springframework.data.repository.query.Param("retryBefore") java.time.LocalDateTime retryBefore,@org.springframework.data.repository.query.Param("claimBefore") java.time.LocalDateTime claimBefore,Pageable page);
}
