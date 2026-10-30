package com.aria.repository;
public interface StoreOrderSnapshotRepository extends org.springframework.data.jpa.repository.JpaRepository<com.aria.entity.StoreOrderSnapshot,Long>{
 java.util.Optional<com.aria.entity.StoreOrderSnapshot> findByStoreIdAndExternalId(Long storeId,String externalId);
 java.util.List<com.aria.entity.StoreOrderSnapshot> findTop100ByStoreIdOrderByModifiedAtUtcDescIdDesc(Long storeId);
 long countByStoreId(Long storeId);
}
