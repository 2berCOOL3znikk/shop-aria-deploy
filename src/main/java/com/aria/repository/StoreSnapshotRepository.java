package com.aria.repository;
public interface StoreSnapshotRepository extends org.springframework.data.jpa.repository.JpaRepository<com.aria.entity.StoreSnapshot,Long> {
 java.util.List<com.aria.entity.StoreSnapshot> findByStoreIdOrderByNameAsc(Long id);
 java.util.Optional<com.aria.entity.StoreSnapshot> findByStoreIdAndExternalId(Long id,String externalId);
 long countByStoreId(Long id);
 void deleteByStoreId(Long id);
}
