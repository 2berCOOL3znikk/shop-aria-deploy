package com.aria.repository;
import com.aria.entity.InventoryLot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface InventoryLotRepository extends JpaRepository<InventoryLot,Long> {
 List<InventoryLot> findByProductId(Long id);
 boolean existsBySupplierId(Long id);
 boolean existsByProductIdAndLotNumber(Long productId,String lotNumber);
}
