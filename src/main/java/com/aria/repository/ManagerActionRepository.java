package com.aria.repository;
import com.aria.entity.ManagerAction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ManagerActionRepository extends JpaRepository<ManagerAction,Long> {
 Optional<ManagerAction> findBySignalKey(String key);
 List<ManagerAction> findTop200ByOrderByLastSeenDescIdDesc();
 List<ManagerAction> findByProductId(Long id);
}
