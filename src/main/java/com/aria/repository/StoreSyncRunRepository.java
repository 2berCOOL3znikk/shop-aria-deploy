package com.aria.repository;
public interface StoreSyncRunRepository extends org.springframework.data.jpa.repository.JpaRepository<com.aria.entity.StoreSyncRun,Long>{
 java.util.List<com.aria.entity.StoreSyncRun> findTop30ByStoreIdOrderByIdDesc(Long id);
}
