package com.aria.controller;

import com.aria.entity.Order;
import com.aria.entity.OrderItem;
import com.aria.entity.Product;
import com.aria.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @Transactional(readOnly = true)
    @GetMapping
    public List<Map<String, Object>> getAll(Authentication authentication) {
        return service.getOrders(authentication.getName()).stream().map(this::orderToMap).collect(Collectors.toList());
    }

    @PostMapping("/checkout")
    public Map<String, Object> checkout(@RequestBody Map<String, String> data, Authentication authentication) {
        String address = data.get("address");
        Order order = service.createOrderFromCart(authentication.getName(), address);
        return orderToMap(order);
    }

    private Map<String, Object> orderToMap(Order order) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", order.getId());
        map.put("createdAt", order.getCreatedAt());
        map.put("totalAmount", order.getTotalAmount());
        map.put("status", order.getStatus());
        map.put("deliveryAddress", order.getDeliveryAddress());
        List<Map<String, Object>> items = order.getItems().stream().map(item -> {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("quantity", item.getQuantity());
            itemMap.put("pricePerUnit", item.getPricePerUnit());
            Product p = item.getProduct();
            if (p != null) {
                itemMap.put("productName", item.getProductName()!=null?item.getProductName():p.getName());
                itemMap.put("productSku", item.getProductSku()!=null?item.getProductSku():p.getSku());
                itemMap.put("imageUrl", p.getImageUrl() != null ? p.getImageUrl() : "/images/product-generic.svg");
            }
            return itemMap;
        }).collect(Collectors.toList());
        map.put("items", items);
        return map;
    }
}
