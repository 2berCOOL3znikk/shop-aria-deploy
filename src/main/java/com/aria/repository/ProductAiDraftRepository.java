package com.aria.repository;
import com.aria.entity.ProductAiDraft;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface ProductAiDraftRepository extends JpaRepository<ProductAiDraft,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select d from ProductAiDraft d where d.id=:id")
    Optional<ProductAiDraft> lockById(@Param("id") Long id);
    Optional<ProductAiDraft> findByProductId(Long id);
}
