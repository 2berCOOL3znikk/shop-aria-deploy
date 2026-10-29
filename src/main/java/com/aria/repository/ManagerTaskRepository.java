package com.aria.repository;
import com.aria.entity.ManagerTask;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.*;
public interface ManagerTaskRepository extends JpaRepository<ManagerTask,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select t from ManagerTask t where t.productId=:product") Optional<ManagerTask> lockProduct(@Param("product") Long product);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select t from ManagerTask t where t.id=:id") Optional<ManagerTask> lockTask(@Param("id") Long id);
 @Query("select t.id from ManagerTask t where (t.status in ('QUEUED','RETRY','WAITING') and t.dueAt<=:now) or (t.status='RUNNING' and t.leaseUntil<=:now) order by t.dueAt,t.id") List<Long> due(@Param("now") LocalDateTime now,org.springframework.data.domain.Pageable page);
 @Query("select t.status,count(t) from ManagerTask t group by t.status") List<Object[]> counts();
 @Modifying(flushAutomatically=true) @Query("update ManagerTask t set t.status='QUEUED',t.generation=t.generation+1,t.attempts=0,t.dueAt=:now,t.updatedAt=:now,t.lease=null,t.leaseUntil=null where t.productId in (select p.id from Product p where p.archived=false and p.status not in :closed)")
 int policyChanged(@Param("now") LocalDateTime now,@Param("closed") Collection<com.aria.enums.ProductStatus> closed);
 @Modifying(flushAutomatically=true) @Query("update ManagerTask t set t.status='QUEUED',t.generation=t.generation+1,t.dueAt=:now,t.updatedAt=:now,t.lease=null,t.leaseUntil=null where t.status='RUNNING'") int releaseRunning(@Param("now") LocalDateTime now);
 @Query("select min(t.dueAt) from ManagerTask t where t.status in ('QUEUED','RETRY')") LocalDateTime nextPending();
 List<ManagerTask> findTop100ByOrderByUpdatedAtDescIdDesc();
}
