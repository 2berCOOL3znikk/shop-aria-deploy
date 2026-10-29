package com.aria.repository;
import com.aria.entity.PurchaseRequest;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select p from PurchaseRequest p where p.id=:id") Optional<PurchaseRequest> lockById(@org.springframework.data.repository.query.Param("id") Long id);
 Optional<PurchaseRequest> findFirstByProductIdAndStatusInOrderByCreatedAtDesc(Long id,Collection<String> states);
 List<PurchaseRequest> findTop100ByOrderByCreatedAtDescIdDesc();
 @Query("select coalesce(sum(p.estimatedTotal),0) from PurchaseRequest p where p.managerCreated=true and p.createdAt>=:since and p.status<>'CANCELLED'") BigDecimal budgetUsed(@org.springframework.data.repository.query.Param("since") LocalDateTime since);
}
