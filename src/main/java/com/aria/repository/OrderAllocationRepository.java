package com.aria.repository;
import com.aria.entity.OrderAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface OrderAllocationRepository extends JpaRepository<OrderAllocation,Long>{List<OrderAllocation> findByOrderId(Long id);}
