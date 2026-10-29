package com.aria.repository;
public interface ConnectedStoreRepository extends org.springframework.data.jpa.repository.JpaRepository<com.aria.entity.ConnectedStore,Long> {
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select s from ConnectedStore s where s.id=:id")
 java.util.Optional<com.aria.entity.ConnectedStore> lockById(@org.springframework.data.repository.query.Param("id") Long id);
}
