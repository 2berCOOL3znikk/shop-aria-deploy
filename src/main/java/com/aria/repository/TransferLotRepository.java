package com.aria.repository;
import com.aria.entity.TransferLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface TransferLotRepository extends JpaRepository<TransferLot, Long> {}