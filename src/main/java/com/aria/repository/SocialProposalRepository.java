package com.aria.repository;
import com.aria.entity.SocialProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface SocialProposalRepository extends JpaRepository<SocialProposal,Long>{
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from SocialProposal p where p.id=:id")
    Optional<SocialProposal> lockById(@org.springframework.data.repository.query.Param("id") Long id);
    Optional<SocialProposal> findByDecisionId(Long decisionId);
    List<SocialProposal> findTop100ByOrderByCreatedAtDescIdDesc();
}
