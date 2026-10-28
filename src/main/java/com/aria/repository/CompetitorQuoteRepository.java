package com.aria.repository;
import com.aria.entity.CompetitorQuote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.*;
public interface CompetitorQuoteRepository extends JpaRepository<CompetitorQuote,Long>{
    Optional<CompetitorQuote> findBySourceIdAndProductIdAndObservedAt(Long sourceId,Long productId,Instant observedAt);
    List<CompetitorQuote> findByProductIdOrderByObservedAtDescIdDesc(Long productId);
}
