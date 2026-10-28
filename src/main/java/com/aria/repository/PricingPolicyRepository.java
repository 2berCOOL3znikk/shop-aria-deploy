package com.aria.repository;
import com.aria.entity.PricingPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface PricingPolicyRepository extends JpaRepository<PricingPolicy,Long> {
    Optional<PricingPolicy> findByCategory(String category);
}
