package com.aria.repository;

import com.aria.entity.AriaLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AriaLogRepository extends JpaRepository<AriaLog, Long> {
    List<AriaLog> findAllByOrderByAnalyzedAtDesc();
}