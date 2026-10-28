package com.aria.repository;
import com.aria.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AuditEventRepository extends JpaRepository<AuditEvent,Long>,org.springframework.data.jpa.repository.JpaSpecificationExecutor<AuditEvent> {
 List<AuditEvent> findTop100ByOrderByCreatedAtDescIdDesc();
}
