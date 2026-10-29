package com.aria.repository;
import com.aria.entity.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import java.time.*;
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement,Long>,org.springframework.data.jpa.repository.JpaSpecificationExecutor<InventoryMovement> {
 Optional<InventoryMovement> findFirstByLotIdAndTypeOrderByIdAsc(Long lotId,String type);
 List<InventoryMovement> findTop200ByOrderByCreatedAtDescIdDesc();
 List<InventoryMovement> findByTypeAndCreatedAtGreaterThanEqual(String type,LocalDateTime since);
}
