package com.aria.service;

import com.aria.entity.CartItem;
import com.aria.entity.Product;
import com.aria.enums.ProductStatus;
import com.aria.repository.CartItemRepository;
import com.aria.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CartService {
    private static final String LEGACY_OWNER = "demo@aria.shop";
    private final CartItemRepository cartRepo;
    private final ProductRepository productRepo;

    public CartService(CartItemRepository cartRepo, ProductRepository productRepo) {
        this.cartRepo = cartRepo;
        this.productRepo = productRepo;
    }

    @Transactional
    public CartItem addToCart(Long productId, int quantity) {
        return addToCart(productId, quantity, LEGACY_OWNER);
    }

    @Transactional
    public CartItem addToCart(Long productId, int quantity, String ownerEmail) {
        if (quantity < 1) throw new IllegalArgumentException("Количество должно быть больше нуля");
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));
        if (product.getStatus() != null && product.getStatus() != ProductStatus.ACTIVE
                && product.getStatus() != ProductStatus.DISCOUNTED) {
            throw new IllegalStateException("Товар недоступен для покупки");
        }

        CartItem item = cartRepo.findByOwnerEmailAndProductId(ownerEmail, productId)
                .orElse(new CartItem(product, 0, priceFor(product), ownerEmail));
        int newQuantity = item.getQuantity() + quantity;
        if (product.getQuantityOnHand() != null && newQuantity > product.getQuantityOnHand()) {
            throw new IllegalArgumentException("На складе доступно только " + product.getQuantityOnHand() + " шт.");
        }
        item.setQuantity(newQuantity);
        item.setPriceAtAddTime(priceFor(product));
        item.setOwnerEmail(ownerEmail);
        return cartRepo.save(item);
    }

    private BigDecimal priceFor(Product product) {
        BigDecimal price = product.getUnitPrice();
        Integer discount = product.getDiscountPercent();
        if (price != null && discount != null && discount > 0) {
            return price.multiply(BigDecimal.valueOf(100 - discount))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return price;
    }

    @Transactional(readOnly = true)
    public List<CartItem> getCartItems() { return cartRepo.findAllByOrderByAddedAtDesc(); }

    @Transactional(readOnly = true)
    public List<CartItem> getCartItems(String ownerEmail) { return cartRepo.findAllByOwnerEmailOrderByAddedAtDesc(ownerEmail); }

    @Transactional
    public void removeFromCart(Long itemId) { cartRepo.deleteById(itemId); }

    @Transactional
    public void removeFromCart(Long itemId, String ownerEmail) { cartRepo.deleteByIdAndOwnerEmail(itemId, ownerEmail); }

    @Transactional
    public CartItem changeQuantity(Long itemId, int quantity, String ownerEmail) {
        CartItem item = cartRepo.findByIdAndOwnerEmail(itemId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Позиция корзины не найдена"));
        if (quantity < 1) {
            cartRepo.delete(item);
            return item;
        }
        if (item.getProduct().getQuantityOnHand() != null && quantity > item.getProduct().getQuantityOnHand()) {
            throw new IllegalArgumentException("На складе доступно только " + item.getProduct().getQuantityOnHand() + " шт.");
        }
        item.setQuantity(quantity);
        return cartRepo.save(item);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateTotal() { return total(getCartItems()); }

    @Transactional(readOnly = true)
    public BigDecimal calculateTotal(String ownerEmail) { return total(getCartItems(ownerEmail)); }

    private BigDecimal total(List<CartItem> items) {
        return items.stream().map(item -> item.getPriceAtAddTime().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public void checkout() { cartRepo.deleteAll(); }

    @Transactional
    public void checkout(String ownerEmail) { cartRepo.deleteAllByOwnerEmail(ownerEmail); }

    public long countItems(String ownerEmail) { return cartRepo.countByOwnerEmail(ownerEmail); }
}
