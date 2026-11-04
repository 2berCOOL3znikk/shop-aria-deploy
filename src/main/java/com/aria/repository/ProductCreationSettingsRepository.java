package com.aria.repository;
import com.aria.entity.ProductCreationSettings;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface ProductCreationSettingsRepository extends JpaRepository<ProductCreationSettings,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from ProductCreationSettings s where s.id=1")
    Optional<ProductCreationSettings> lockSettings();
}
