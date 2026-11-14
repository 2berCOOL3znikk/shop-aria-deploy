package com.aria.repository;
import com.aria.entity.WorkspacePoolSlot;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface WorkspacePoolSlotRepository extends JpaRepository<WorkspacePoolSlot,String> {
 long countByState(String state);
 Optional<WorkspacePoolSlot> findByAdmissionHash(String hash);
 Optional<WorkspacePoolSlot> findByBinding(String binding);
 Optional<WorkspacePoolSlot> findByOrigin(String origin);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from WorkspacePoolSlot s where s.id=:id")
 Optional<WorkspacePoolSlot> lockById(String id);
 List<WorkspacePoolSlot> findTop1ByStateOrderByCreatedAtAsc(String state);
}
