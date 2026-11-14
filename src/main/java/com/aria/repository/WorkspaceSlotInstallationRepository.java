package com.aria.repository;
import com.aria.entity.WorkspaceSlotInstallation;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface WorkspaceSlotInstallationRepository extends JpaRepository<WorkspaceSlotInstallation,Integer> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from WorkspaceSlotInstallation s where s.id=1")
 Optional<WorkspaceSlotInstallation> lockSingleton();
}
