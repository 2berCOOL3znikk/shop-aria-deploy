package com.aria.repository;
import com.aria.entity.WorkspaceActivation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;
public interface WorkspaceActivationRepository extends JpaRepository<WorkspaceActivation,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select a from WorkspaceActivation a where a.id=1")
 Optional<WorkspaceActivation> lockSingleton();
}
