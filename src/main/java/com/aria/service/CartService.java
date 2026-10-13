package com.aria.service;

import com.aria.entity.CartItem;
import com.aria.entity.Product;
import com.aria.repository.CartItemRepository;
import com.aria.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {

    private final CartItemRepository cartRepo;
    private final ProductRepository productRepo;

    public CartService(CartItemRepository cartRepo, ProductRepository productRepo) {
        this.cartRepo = cartRepo;
        this.productRepo = productRepo;
    }

    @Transactional
    public CartItem addToCart(Long productId, int quantity) {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

        if (product.getStatus() != null && !product.getStatus().name().equals("ACTIVE")) {
            throw new IllegalStateException("Товар недоступен для покупки");
        }

        CartItem item = new CartItem(product, quantity, product.getUnitPrice());
        return cartRepo.save(item);
    }

    @Transactional(readOnly = true)
    public List<CartItem> getCartItems() {
        return cartRepo.findAllByOrderByAddedAtDesc();
    }

    @Transactional
    public void removeFromCart(Long itemId) {
        cartRepo.deleteById(itemId);
    }

    @Transactional
    public BigDecimal calculateTotal() {
        return getCartItems().stream()
                .map(item -> item.getPriceAtAddTime().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public void checkout() {
        // Имитация оформления заказа: очищаем корзину
        // В реальном проде здесь создание Order, списание со склада, отправка письма
        cartRepo.deleteAll();
        System.out.println("[CART] Заказ оформлен. Корзина очищена.");
    }
}