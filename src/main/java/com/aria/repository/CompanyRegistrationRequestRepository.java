package com.aria.repository;

import com.aria.entity.CompanyRegistrationRequest;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface CompanyRegistrationRequestRepository extends JpaRepository<CompanyRegistrationRequest,String> {
 List<CompanyRegistrationRequest> findTop100ByOrderByCreatedAtDesc();
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select r from CompanyRegistrationRequest r where r.id=:id")
 Optional<CompanyRegistrationRequest> lockById(@Param("id") String id);
}
