package com.aria.repository;
public interface ExternalIntegrationRepository extends org.springframework.data.jpa.repository.JpaRepository<com.aria.entity.ExternalIntegration,Long> {
 java.util.Optional<com.aria.entity.ExternalIntegration> findByStoreId(Long id);
}
