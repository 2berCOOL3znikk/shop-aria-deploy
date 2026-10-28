package com.aria.repository;

import com.aria.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from CartItem c where c.ownerEmail=:owner order by c.product.id")
    List<CartItem> lockForCheckout(@org.springframework.data.repository.query.Param("owner") String owner);
    List<CartItem> findAllByOrderByAddedAtDesc();
    List<CartItem> findAllByOwnerEmailOrderByAddedAtDesc(String ownerEmail);
    java.util.Optional<CartItem> findByOwnerEmailAndProductId(String ownerEmail, Long productId);
    java.util.Optional<CartItem> findByIdAndOwnerEmail(Long id, String ownerEmail);
    long countByOwnerEmail(String ownerEmail);
    void deleteAllByOwnerEmail(String ownerEmail);
    void deleteByIdAndOwnerEmail(Long id, String ownerEmail);
}
