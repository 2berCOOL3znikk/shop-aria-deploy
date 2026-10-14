package com.aria.controller;

import com.aria.entity.CartItem;
import com.aria.entity.Product;
import com.aria.service.CartService;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
    public List<Map<String, Object>> getCart(Authentication authentication) {
        return service.getCartItems(authentication.getName()).stream().map(item -> {
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
                map.put("imageUrl", p.getImageUrl() != null ? p.getImageUrl() : "/images/product-generic.svg");
            }
            return map;
        }).collect(Collectors.toList());
    }

    @PostMapping("/add")
    public Map<String, Object> add(@RequestParam Long productId, @RequestParam(defaultValue = "1") int qty, Authentication authentication) {
        CartItem item = service.addToCart(productId, qty, authentication.getName());
        return Map.of("id", item.getId(), "quantity", item.getQuantity(), "priceAtAddTime", item.getPriceAtAddTime());
    }

    @DeleteMapping("/{itemId}")
    public void remove(@PathVariable Long itemId, Authentication authentication) {
        service.removeFromCart(itemId, authentication.getName());
    }

    @PutMapping("/{itemId}")
    public Map<String, Object> changeQuantity(@PathVariable Long itemId, @RequestParam int qty, Authentication authentication) {
        CartItem item = service.changeQuantity(itemId, qty, authentication.getName());
        return Map.of("id", item.getId(), "quantity", Math.max(0, item.getQuantity()));
    }

    @Transactional(readOnly = true)
    @GetMapping("/total")
    public Map<String, Object> getTotal(Authentication authentication) {
        BigDecimal total = service.calculateTotal(authentication.getName());
        Map<String, Object> result = new HashMap<>();
        result.put("totalAmount", total);
        result.put("itemCount", service.countItems(authentication.getName()));
        return result;
    }

    @PostMapping("/checkout")
    public void checkout(Authentication authentication) {
        service.checkout(authentication.getName());
    }
}
