package com.aria.repository;
import com.aria.entity.SmartPricingSettings;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface SmartPricingSettingsRepository extends JpaRepository<SmartPricingSettings,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from SmartPricingSettings s where s.id=1")
    Optional<SmartPricingSettings> lockSettings();
}
