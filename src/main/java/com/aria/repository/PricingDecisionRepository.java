package com.aria.repository;
import com.aria.entity.PricingDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import java.util.*;
public interface PricingDecisionRepository extends JpaRepository<PricingDecision,Long> {
    Optional<PricingDecision> findFirstByProductIdOrderByCreatedAtDescIdDesc(Long productId);
    List<PricingDecision> findByProductIdAndStatus(Long productId,String status);
    Page<PricingDecision> findAllByOrderByCreatedAtDescIdDesc(Pageable page);
}
