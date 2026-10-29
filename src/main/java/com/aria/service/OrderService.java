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
    @org.springframework.beans.factory.annotation.Autowired private org.springframework.context.ApplicationEventPublisher events;
    private static final String LEGACY_OWNER = "demo@aria.shop";

    private final OrderRepository orderRepo;
    private final CartItemRepository cartRepo;
    private final ProductRepository productRepo;
    private final InventoryService inventory;
    private final PricingService pricing;
    private final AuditService audit;
    private final com.aria.repository.OrderAllocationRepository allocations;

    public OrderService(OrderRepository orderRepo, CartItemRepository cartRepo, ProductRepository productRepo, InventoryService inventory, PricingService pricing, AuditService audit, com.aria.repository.OrderAllocationRepository allocations) {
        this.orderRepo = orderRepo;
        this.cartRepo = cartRepo;
        this.productRepo = productRepo;
        this.inventory=inventory; this.pricing=pricing;this.audit=audit;
        this.allocations=allocations;
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
        if(address==null || address.isBlank() || address.length()>500) throw new IllegalArgumentException("Укажите адрес доставки (до 500 символов)");
        List<CartItem> cartItems = cartRepo.lockForCheckout(ownerEmail);

        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Корзина пуста");
        }

        Order order = new Order();
        order.setDeliveryAddress(address);
        order.setOwnerEmail(ownerEmail);
        order.setStatus("PENDING");

        for (CartItem ci : cartItems) {
            Product product = inventory.lock(ci.getProduct().getId());
            if(product.getArchived() || inventory.available(product)<ci.getQuantity()) throw new IllegalStateException("Товар недоступен или срок партии истёк: "+product.getName());
            if(pricing.price(product,product.getDiscountPercent()==null?0:product.getDiscountPercent()).compareTo(ci.getPriceAtAddTime())!=0) throw new IllegalStateException("Цена товара «"+product.getName()+"» изменилась. Удалите его из корзины и добавьте заново");
            if (product.getStatus() != com.aria.enums.ProductStatus.ACTIVE
                    && product.getStatus() != com.aria.enums.ProductStatus.DISCOUNTED) {
                throw new IllegalStateException("Товар «" + product.getName() + "» больше не продаётся");
            }
            if (product.getQuantityOnHand() == null || product.getQuantityOnHand() < ci.getQuantity()) {
                throw new IllegalStateException("Недостаточный остаток товара «" + product.getName() + "»");
            }
        }

        BigDecimal total = BigDecimal.ZERO;
        java.util.List<InventoryService.Consumption> consumed=new java.util.ArrayList<>();

        for (CartItem ci : cartItems) {
            Product product = ci.getProduct();

            // Создаем позицию заказа
            OrderItem oi = new OrderItem(product, ci.getQuantity(), ci.getPriceAtAddTime());
            order.getItems().add(oi);

            // Считаем сумму
            BigDecimal lineTotal = ci.getPriceAtAddTime().multiply(BigDecimal.valueOf(ci.getQuantity()));
            total = total.add(lineTotal);

            // Списываем со склада (уменьшаем остаток)
            consumed.addAll(inventory.consume(product,ci.getQuantity(),"ORDER_STOCK"));
        }

        order.setTotalAmount(total);
        Order savedOrder = orderRepo.save(order);
        for(var allocation:consumed) allocations.save(new com.aria.entity.OrderAllocation(savedOrder,allocation.lot(),allocation.quantity()));
        audit.record("ORDER_CREATED","order:"+savedOrder.getId(),"Сумма "+total+" ₸ · "+ownerEmail);

        // Очищаем корзину после оформления
        cartRepo.deleteAllByOwnerEmail(ownerEmail);

        System.out.println("[ORDER] Заказ #" + savedOrder.getId() + " оформлен. Сумма: " + total + " ₸");

        return savedOrder;
    }
    @Transactional
    public Order changeStatus(Long id,String next,String reason) {
        Order order=orderRepo.findLockedById(id).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Заказ не найден"));
        String current=order.getStatus();
        if(!(("PENDING".equals(current)&&("SHIPPED".equals(next)||"CANCELLED".equals(next))) || ("SHIPPED".equals(current)&&"DELIVERED".equals(next)))) throw new IllegalStateException("Недопустимое изменение статуса заказа");
        if("CANCELLED".equals(next)) {
            var rows=allocations.findByOrderId(id);
            if(rows.isEmpty()) throw new IllegalStateException("Для старого заказа нет данных о партиях. Требуется ручная сверка склада");
            rows.sort(java.util.Comparator.comparing(r->r.getLot().getProduct().getId()));
            for(var row:rows) inventory.restore(row.getLot(),row.getQuantity());
        }
        if("SHIPPED".equals(next))order.setShippedAt(LocalDateTime.now());
        order.setStatus(next);for(var item:order.getItems())events.publishEvent(new StoreChanged(item.getProduct().getId(),"ORDER_STATUS_CHANGED"));audit.record("ORDER_STATUS_CHANGED","order:"+id,current+" → "+next+" · "+reason);return order;
    }
}
