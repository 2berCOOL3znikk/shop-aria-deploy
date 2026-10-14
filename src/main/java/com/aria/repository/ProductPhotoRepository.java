package com.aria.repository;

import com.aria.entity.ProductPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductPhotoRepository extends JpaRepository<ProductPhoto, Long> {
    Optional<ProductPhoto> findByProductId(Long productId);
}
