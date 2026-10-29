package com.aria.repository;

import com.aria.entity.Product;
import com.aria.enums.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    java.util.Optional<Product> findBySkuIgnoreCase(String sku);
    boolean existsBySkuIgnoreCase(String sku);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    java.util.Optional<Product> lockById(@Param("id") Long id);

    List<Product> findByStatus(ProductStatus status);
    List<Product> findByArchivedFalseAndStatusInAndIdGreaterThanOrderByIdAsc(
        java.util.Collection<ProductStatus> statuses,Long cursor,org.springframework.data.domain.Pageable page);

    // НОВЫЙ МЕТОД ПОИСКА
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.category) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Product> searchProducts(@Param("query") String query);
}
