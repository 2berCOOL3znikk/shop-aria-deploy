package com.aria.repository;
import com.aria.entity.PosReceipt;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface PosReceiptRepository extends JpaRepository<PosReceipt,Long> {
 Optional<PosReceipt> findByRequestKey(String key);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select p from PosReceipt p where p.id=:id") Optional<PosReceipt> lockById(@org.springframework.data.repository.query.Param("id") Long id);
 List<PosReceipt> findTop100ByOrderByCreatedAtDescIdDesc();
}
