package com.aria.repository;
import com.aria.entity.ManagerRun;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ManagerRunRepository extends JpaRepository<ManagerRun,Long> {
 Optional<ManagerRun> findByLease(String lease);
 List<ManagerRun> findTop20ByOrderByStartedAtDescIdDesc();
}
