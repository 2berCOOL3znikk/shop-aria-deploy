package com.aria.repository;
import com.aria.entity.InventoryLot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface InventoryLotRepository extends JpaRepository<InventoryLot,Long> {
 @org.springframework.data.jpa.repository.Query("select coalesce(sum(l.quantity*l.unitCost),0),coalesce(sum(l.quantity),0) from InventoryLot l where l.active=true and l.quantity>0") List<Object[]> heldValue();
 List<InventoryLot> findByProductId(Long id);
 boolean existsBySupplierId(Long id);
 boolean existsByProductIdAndLotNumber(Long productId,String lotNumber);
}
