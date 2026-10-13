package com.aria.service;

import com.aria.entity.Product;
import com.aria.entity.WishlistItem;
import com.aria.repository.ProductRepository;
import com.aria.repository.WishlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepo;
    private final ProductRepository productRepo;

    public WishlistService(WishlistRepository wishlistRepo, ProductRepository productRepo) {
        this.wishlistRepo = wishlistRepo;
        this.productRepo = productRepo;
    }

    public List<WishlistItem> getAll() {
        return wishlistRepo.findAllByOrderByAddedAtDesc();
    }

    @Transactional
    public WishlistItem toggleLike(Long productId) {
        var existing = wishlistRepo.findByProductId(productId);

        if (existing.isPresent()) {
            wishlistRepo.delete(existing.get());
            return null; // Удалено из избранного
        } else {
            Product product = productRepo.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));
            WishlistItem item = new WishlistItem(product);
            return wishlistRepo.save(item);
        }
    }

    public boolean isLiked(Long productId) {
        return wishlistRepo.existsByProductId(productId);
    }
}