package com.aria.repository;
import com.aria.entity.SupplierQuote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface SupplierQuoteRepository extends JpaRepository<SupplierQuote,Long> {
 Optional<SupplierQuote> findByProductIdAndSupplierId(Long productId,Long supplierId);
 List<SupplierQuote> findByProductId(Long productId);
}
