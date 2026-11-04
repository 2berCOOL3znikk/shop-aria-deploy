package com.aria.service;

import com.aria.entity.CartItem;
import com.aria.entity.Product;
import com.aria.enums.ProductStatus;
import com.aria.repository.CartItemRepository;
import com.aria.repository.ProductRepository;
import com.aria.repository.AppUserRepository;
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
    private final InventoryService inventory;
    private final AppUserRepository owners;

    public CartService(CartItemRepository cartRepo, ProductRepository productRepo, InventoryService inventory, AppUserRepository owners) {
        this.cartRepo = cartRepo;
        this.productRepo = productRepo;
        this.inventory = inventory;
        this.owners = owners;
    }

    @Transactional
    public CartItem addToCart(Long productId, int quantity) {
        return addToCart(productId, quantity, LEGACY_OWNER);
    }

    @Transactional
    public CartItem addToCart(Long productId, int quantity, String ownerEmail) {
        if (quantity < 1) throw new IllegalArgumentException("Количество должно быть больше нуля");
        lockOwner(ownerEmail);
        Product product = inventory.lock(productId);
        if (product.getArchived()) throw new IllegalStateException("Товар недоступен для покупки");
        if (product.getStatus() != null && product.getStatus() != ProductStatus.ACTIVE
                && product.getStatus() != ProductStatus.DISCOUNTED) {
            throw new IllegalStateException("Товар недоступен для покупки");
        }

        CartItem item = cartRepo.findByOwnerEmailAndProductId(ownerEmail, productId)
                .orElse(new CartItem(product, 0, priceFor(product), ownerEmail));
        long newQuantity = (long)item.getQuantity() + quantity;
        int available=inventory.available(product);
        if (newQuantity > available) {
            throw new IllegalArgumentException("На складе доступно только " + available + " шт.");
        }
        item.setQuantity((int)newQuantity);
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
    public void removeFromCart(Long itemId) { removeFromCart(itemId,LEGACY_OWNER); }

    @Transactional
    public void removeFromCart(Long itemId, String ownerEmail) { lockOwner(ownerEmail);cartRepo.deleteByIdAndOwnerEmail(itemId, ownerEmail); }

    @Transactional
    public CartItem changeQuantity(Long itemId, int quantity, String ownerEmail) {
        lockOwner(ownerEmail);
        CartItem item = cartRepo.findByIdAndOwnerEmail(itemId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Позиция корзины не найдена"));
        if (quantity < 1) {
            cartRepo.delete(item);
            item.setQuantity(0);
            return item;
        }
        Product product=inventory.lock(item.getProduct().getId());
        int available=inventory.available(product);
        if (quantity > available) {
            throw new IllegalArgumentException("На складе доступно только " + available + " шт.");
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
    public void checkout() { checkout(LEGACY_OWNER); }

    @Transactional
    public void checkout(String ownerEmail) { lockOwner(ownerEmail);cartRepo.deleteAllByOwnerEmail(ownerEmail); }

    private void lockOwner(String email){
        // Serialize even when the cart is empty: there is no cart row to lock before the first addition.
        owners.lockByEmail(email).orElseThrow(()->new IllegalStateException("Учётная запись недоступна. Войдите снова."));
    }

    public long countItems(String ownerEmail) { return cartRepo.countByOwnerEmail(ownerEmail); }
}
