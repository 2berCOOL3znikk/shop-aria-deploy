package com.aria.repository;

import com.aria.entity.SocialOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SocialOfferRepository extends JpaRepository<SocialOffer, Long> {

    // Открытые предложения (доступны для просмотра)
    List<SocialOffer> findByStatusOrderByCreatedAtDesc(String status);

    // Предложения по продукту
    List<SocialOffer> findByProductId(Long productId);

    List<SocialOffer> findByOrgIdAndStatusOrderByCreatedAtDesc(Long orgId, String status);
    java.util.Optional<SocialOffer> findByIdAndOrgIdAndStatus(Long id, Long orgId, String status);
}
