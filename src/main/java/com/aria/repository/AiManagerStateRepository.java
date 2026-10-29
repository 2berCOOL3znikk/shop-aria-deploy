package com.aria.repository;
import com.aria.entity.AiManagerState;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface AiManagerStateRepository extends JpaRepository<AiManagerState,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from AiManagerState s where s.id=1") Optional<AiManagerState> lockState();
}
