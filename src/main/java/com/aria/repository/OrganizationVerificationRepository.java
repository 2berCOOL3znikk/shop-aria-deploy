package com.aria.repository;

import com.aria.entity.OrganizationVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrganizationVerificationRepository extends JpaRepository<OrganizationVerification, Long> {
    Optional<OrganizationVerification> findByUserId(Long userId);
    List<OrganizationVerification> findAllByStatusOrderBySubmittedAtAsc(String status);
}