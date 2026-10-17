package com.aria.service;

import com.aria.entity.Product;
import com.aria.entity.WishlistItem; // Переиспользуем существующую сущность
import com.aria.repository.ProductRepository;
import com.aria.repository.WishlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FavoriteServicebak {
    private static final String LEGACY_OWNER = "demo@aria.shop";

    private final WishlistRepository wishlistRepo;
    private final ProductRepository productRepo;

    public FavoriteServicebak(WishlistRepository wishlistRepo, ProductRepository productRepo) {
        this.wishlistRepo = wishlistRepo;
        this.productRepo = productRepo;
    }

    /**
     * Получить все избранные товары пользователя (в демо — одного глобального пользователя).
     */
    public List<Product> getAllFavorites() {
        return wishlistRepo.findAll().stream()
                .map(WishlistItem::getProduct)
                .collect(Collectors.toList());
    }

    public List<Product> getAllFavorites(String ownerEmail) {
        return wishlistRepo.findByOwnerEmailOrderByAddedAtDesc(ownerEmail).stream()
                .map(WishlistItem::getProduct).collect(Collectors.toList());
    }

    /**
     * Добавить товар в избранное.
     */
    @Transactional
    public void addFavorite(Long productId) {
        if (wishlistRepo.findByProductId(productId).isPresent()) {
            return; // Уже есть, ничего не делаем
        }

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

        WishlistItem item = new WishlistItem(product);
        wishlistRepo.save(item);
    }

    @Transactional
    public void addFavorite(Long productId, String ownerEmail) {
        if (wishlistRepo.findByOwnerEmailAndProductId(ownerEmail, productId).isPresent()) return;
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));
        wishlistRepo.save(new WishlistItem(product, ownerEmail));
    }

    /**
     * Удалить товар из избранного.
     */
    @Transactional
    public void removeFavorite(Long productId) {
        wishlistRepo.findByProductId(productId).ifPresent(wishlistRepo::delete);
    }

    @Transactional
    public void removeFavorite(Long productId, String ownerEmail) {
        wishlistRepo.findByOwnerEmailAndProductId(ownerEmail, productId).ifPresent(wishlistRepo::delete);
    }

    /**
     * Проверить, является ли товар избранным.
     */
    public boolean isFavorite(Long productId) {
        return wishlistRepo.existsByProductId(productId);
    }

    public boolean isFavorite(Long productId, String ownerEmail) {
        return wishlistRepo.existsByOwnerEmailAndProductId(ownerEmail, productId);
    }
}
