package com.aria.repository;
public interface StoreWebhookReceiverRepository extends org.springframework.data.jpa.repository.JpaRepository<com.aria.entity.StoreWebhookReceiver,Long>{
 java.util.Optional<com.aria.entity.StoreWebhookReceiver> findByRouteToken(String token);
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select r from StoreWebhookReceiver r where r.routeToken=:token")
 java.util.Optional<com.aria.entity.StoreWebhookReceiver> lockByToken(@org.springframework.data.repository.query.Param("token") String token);
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select r from StoreWebhookReceiver r where r.storeId=:id")
 java.util.Optional<com.aria.entity.StoreWebhookReceiver> lockByStoreId(@org.springframework.data.repository.query.Param("id") Long id);
}
