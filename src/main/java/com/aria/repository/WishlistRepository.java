package com.aria.repository;

import com.aria.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<WishlistItem, Long> {
    List<WishlistItem> findAllByOrderByAddedAtDesc();
    Optional<WishlistItem> findByProductId(Long productId);
    boolean existsByProductId(Long productId);
    List<WishlistItem> findByOwnerEmailOrderByAddedAtDesc(String ownerEmail);
    Optional<WishlistItem> findByOwnerEmailAndProductId(String ownerEmail, Long productId);
    boolean existsByOwnerEmailAndProductId(String ownerEmail, Long productId);
}
