package com.aria.repository;
public interface StoreSyncTaskRepository extends org.springframework.data.jpa.repository.JpaRepository<com.aria.entity.StoreSyncTask,Long>{
 java.util.Optional<com.aria.entity.StoreSyncTask> findByStoreIdAndOperation(Long storeId,String operation);
 java.util.List<com.aria.entity.StoreSyncTask> findByStoreIdOrderByIdAsc(Long storeId);
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select t from StoreSyncTask t where t.id=:id")
 java.util.Optional<com.aria.entity.StoreSyncTask> lockById(@org.springframework.data.repository.query.Param("id") Long id);
}
