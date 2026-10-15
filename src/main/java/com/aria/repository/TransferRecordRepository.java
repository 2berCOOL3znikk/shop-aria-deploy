package com.aria.repository;

import com.aria.entity.TransferRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransferRecordRepository extends JpaRepository<TransferRecord, Long> {
    List<TransferRecord> findByLotIdOrderByIdAsc(Long lotId);
    List<TransferRecord> findAllByOrderByTransferredAtDesc();
}