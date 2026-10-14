package com.aria.service;

import com.aria.entity.CartItem;
import com.aria.entity.Order;
import com.aria.entity.OrderItem;
import com.aria.entity.Product;
import com.aria.repository.CartItemRepository;
import com.aria.repository.OrderRepository;
import com.aria.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {
    private static final String LEGACY_OWNER = "demo@aria.shop";

    private final OrderRepository orderRepo;
    private final CartItemRepository cartRepo;
    private final ProductRepository productRepo;

    public OrderService(OrderRepository orderRepo, CartItemRepository cartRepo, ProductRepository productRepo) {
        this.orderRepo = orderRepo;
        this.cartRepo = cartRepo;
        this.productRepo = productRepo;
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Order> getOrders(String ownerEmail) {
        return orderRepo.findByOwnerEmailOrderByCreatedAtDesc(ownerEmail);
    }

    @Transactional
    public Order createOrderFromCart(String address) {
        return createOrderFromCart(LEGACY_OWNER, address);
    }

    @Transactional
    public Order createOrderFromCart(String ownerEmail, String address) {
        List<CartItem> cartItems = cartRepo.findAllByOwnerEmailOrderByAddedAtDesc(ownerEmail);

        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Корзина пуста");
        }

        Order order = new Order();
        order.setDeliveryAddress(address);
        order.setOwnerEmail(ownerEmail);
        order.setStatus("PENDING");

        for (CartItem ci : cartItems) {
            Product product = ci.getProduct();
            if (product.getStatus() != com.aria.enums.ProductStatus.ACTIVE
                    && product.getStatus() != com.aria.enums.ProductStatus.DISCOUNTED) {
                throw new IllegalStateException("Товар «" + product.getName() + "» больше не продаётся");
            }
            if (product.getQuantityOnHand() == null || product.getQuantityOnHand() < ci.getQuantity()) {
                throw new IllegalStateException("Недостаточный остаток товара «" + product.getName() + "»");
            }
        }

        BigDecimal total = BigDecimal.ZERO;

        for (CartItem ci : cartItems) {
            Product product = ci.getProduct();

            // Создаем позицию заказа
            OrderItem oi = new OrderItem(product, ci.getQuantity(), ci.getPriceAtAddTime());
            order.getItems().add(oi);

            // Считаем сумму
            BigDecimal lineTotal = ci.getPriceAtAddTime().multiply(BigDecimal.valueOf(ci.getQuantity()));
            total = total.add(lineTotal);

            // Списываем со склада (уменьшаем остаток)
            product.setQuantityOnHand(product.getQuantityOnHand() - ci.getQuantity());
            productRepo.save(product);
        }

        order.setTotalAmount(total);
        Order savedOrder = orderRepo.save(order);

        // Очищаем корзину после оформления
        cartRepo.deleteAllByOwnerEmail(ownerEmail);

        System.out.println("[ORDER] Заказ #" + savedOrder.getId() + " оформлен. Сумма: " + total + " ₸");

        return savedOrder;
    }
}
