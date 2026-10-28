package com.aria.repository;

import com.aria.entity.SocialOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SocialOfferRepository extends JpaRepository<SocialOffer, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select o from SocialOffer o where o.id=:id")
    java.util.Optional<SocialOffer> lockById(@org.springframework.data.repository.query.Param("id") Long id);
    List<SocialOffer> findByOrgIdOrderByCreatedAtDesc(Long id);

    // Открытые предложения (доступны для просмотра)
    List<SocialOffer> findByStatusOrderByCreatedAtDesc(String status);

    // Предложения по продукту
    List<SocialOffer> findByProductId(Long productId);

    List<SocialOffer> findByOrgIdAndStatusOrderByCreatedAtDesc(Long orgId, String status);
    java.util.Optional<SocialOffer> findByIdAndOrgIdAndStatus(Long id, Long orgId, String status);
}
