package com.aria.repository;
import com.aria.entity.PriceChange;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface PriceChangeRepository extends JpaRepository<PriceChange,Long> {
    List<PriceChange> findByProductIdOrderByChangedAtDescIdDesc(Long productId);
    Optional<PriceChange> findFirstByProductIdOrderByChangedAtDescIdDesc(Long productId);
}
