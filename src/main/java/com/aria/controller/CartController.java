package com.aria.controller;

import com.aria.entity.CartItem;
import com.aria.entity.Product;
import com.aria.service.CartService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartService service;

    public CartController(CartService service) {
        this.service = service;
    }

    @Transactional(readOnly = true)
    @GetMapping
    public List<Map<String, Object>> getCart() {
        return service.getCartItems().stream().map(item -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", item.getId());
            map.put("quantity", item.getQuantity());
            map.put("priceAtAddTime", item.getPriceAtAddTime());
            Product p = item.getProduct();
            if (p != null) {
                map.put("productName", p.getName());
                map.put("productSku", p.getSku());
                map.put("productId", p.getId());
                map.put("productPrice", p.getUnitPrice());
                map.put("imageUrl", "https://placehold.co/400x300/1e293b/3b82f6?text=" + URLEncoder.encode(p.getName(), StandardCharsets.UTF_8));
            }
            return map;
        }).collect(Collectors.toList());
    }

    @PostMapping("/add")
    public CartItem add(@RequestParam Long productId, @RequestParam(defaultValue = "1") int qty) {
        return service.addToCart(productId, qty);
    }

    @DeleteMapping("/{itemId}")
    public void remove(@PathVariable Long itemId) {
        service.removeFromCart(itemId);
    }

    @Transactional(readOnly = true)
    @GetMapping("/total")
    public Map<String, Object> getTotal() {
        BigDecimal total = service.calculateTotal();
        Map<String, Object> result = new HashMap<>();
        result.put("totalAmount", total);
        result.put("itemCount", service.getCartItems().size());
        return result;
    }

    @PostMapping("/checkout")
    public void checkout() {
        service.checkout();
    }
}