package com.aria.repository;
import com.aria.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AuditEventRepository extends JpaRepository<AuditEvent,Long> {
 List<AuditEvent> findTop100ByOrderByCreatedAtDescIdDesc();
}
