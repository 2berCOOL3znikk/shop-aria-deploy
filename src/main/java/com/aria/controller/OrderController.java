package com.aria.controller;

import com.aria.entity.Order;
import com.aria.entity.OrderItem;
import com.aria.entity.Product;
import com.aria.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    public List<Map<String, Object>> getAll() {
        return service.getAllOrders().stream().map(this::orderToMap).collect(Collectors.toList());
    }

    @PostMapping("/checkout")
    public Map<String, Object> checkout(@RequestBody Map<String, String> data) {
        String address = data.getOrDefault("address", "г. Москва, ул. Пушкина, д. Колотушкина");
        Order order = service.createOrderFromCart(address);
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
                itemMap.put("productName", p.getName());
                itemMap.put("productSku", p.getSku());
                itemMap.put("imageUrl", "https://placehold.co/400x300/1e293b/3b82f6?text=" + URLEncoder.encode(p.getName(), StandardCharsets.UTF_8));
            }
            return itemMap;
        }).collect(Collectors.toList());
        map.put("items", items);
        return map;
    }
}