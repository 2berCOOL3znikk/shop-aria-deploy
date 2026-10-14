package com.aria.repository;

import com.aria.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findAllByOrderByAddedAtDesc();
    List<CartItem> findAllByOwnerEmailOrderByAddedAtDesc(String ownerEmail);
    java.util.Optional<CartItem> findByOwnerEmailAndProductId(String ownerEmail, Long productId);
    java.util.Optional<CartItem> findByIdAndOwnerEmail(Long id, String ownerEmail);
    long countByOwnerEmail(String ownerEmail);
    void deleteAllByOwnerEmail(String ownerEmail);
    void deleteByIdAndOwnerEmail(Long id, String ownerEmail);
}
