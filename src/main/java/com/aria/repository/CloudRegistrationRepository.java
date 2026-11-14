package com.aria.repository;
import com.aria.entity.CloudRegistration;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface CloudRegistrationRepository extends JpaRepository<CloudRegistration,String> {
 Optional<CloudRegistration> findByEmailHash(String hash);
 boolean existsByAccessHash(String hash);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from CloudRegistration r where r.id=:id")
 Optional<CloudRegistration> lockById(String id);
 List<CloudRegistration> findTop10ByStatusNotOrderByCreatedAtAsc(String status);
}
