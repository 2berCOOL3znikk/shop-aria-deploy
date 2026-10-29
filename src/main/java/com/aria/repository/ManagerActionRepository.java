package com.aria.repository;
import com.aria.entity.ManagerAction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ManagerActionRepository extends JpaRepository<ManagerAction,Long> {
 Optional<ManagerAction> findBySignalKey(String key);
 List<ManagerAction> findTop200ByOrderByLastSeenDescIdDesc();
 List<ManagerAction> findByProductId(Long id);
 long countByStatus(String status);
 @org.springframework.data.jpa.repository.Modifying(flushAutomatically=true)
 @org.springframework.data.jpa.repository.Query("update ManagerAction a set a.status='RESOLVED',a.resolvedAt=:now,a.version=a.version+1 where a.status<>'RESOLVED' and a.productId in (select p.id from Product p where p.archived=true or p.status in :closed)")
 int resolveClosed(@org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now,@org.springframework.data.repository.query.Param("closed") java.util.Collection<com.aria.enums.ProductStatus> closed);
}
