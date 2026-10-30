package com.aria.repository;
public interface StoreWebhookReceiptRepository extends org.springframework.data.jpa.repository.JpaRepository<com.aria.entity.StoreWebhookReceipt,Long>{
 boolean existsByStoreIdAndGenerationAndResourceAndBodyHash(Long storeId,String generation,String resource,String hash);
 long countByStoreId(Long storeId);
 java.util.List<com.aria.entity.StoreWebhookReceipt> findTop20ByStoreIdOrderByIdDesc(Long storeId);
 boolean existsByStatus(String status);
 java.util.Optional<com.aria.entity.StoreWebhookReceipt> findFirstByStatusOrderByIdAsc(String status);
 java.util.List<com.aria.entity.StoreWebhookReceipt> findTop100ByStoreIdAndStatusOrderByIdAsc(Long storeId,String status);
}
