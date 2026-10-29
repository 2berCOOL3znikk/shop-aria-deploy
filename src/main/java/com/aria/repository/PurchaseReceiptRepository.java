package com.aria.repository;
import com.aria.entity.PurchaseReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface PurchaseReceiptRepository extends JpaRepository<PurchaseReceipt,Long> {
 Optional<PurchaseReceipt> findByRequestKey(String key);
 List<PurchaseReceipt> findByRequestIdOrderByReceivedAtAsc(Long id);
}
