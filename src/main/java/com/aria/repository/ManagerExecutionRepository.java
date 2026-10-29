package com.aria.repository;
import com.aria.entity.ManagerExecution;
import org.springframework.data.jpa.repository.*;
import java.util.*;
import java.time.LocalDateTime;
public interface ManagerExecutionRepository extends JpaRepository<ManagerExecution,Long> {
 Optional<ManagerExecution> findByLease(String lease);
 @Modifying(flushAutomatically=true) @Query("update ManagerExecution e set e.status='CANCELLED',e.finishedAt=:now,e.summary='Политика владельца изменилась; старая аренда отозвана' where e.status='RUNNING'") int cancelRunning(@org.springframework.data.repository.query.Param("now") LocalDateTime now);
 List<ManagerExecution> findTop50ByOrderByStartedAtDescIdDesc();
 @Query("select count(e),count(distinct e.productId),sum(case when e.priceApplied=true then 1 else 0 end),sum(case when e.purchaseDraft=true then 1 else 0 end),sum(case when e.socialDraft=true then 1 else 0 end),count(distinct case when e.source='AI DECISION' then e.decisionId else null end) from ManagerExecution e where e.finishedAt>=:from and e.status='DONE'") List<Object[]> today(@org.springframework.data.repository.query.Param("from") LocalDateTime from);
}
