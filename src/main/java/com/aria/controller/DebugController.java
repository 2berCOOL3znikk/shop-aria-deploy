package com.aria.controller;

import com.aria.entity.Product;
import com.aria.entity.ProductDetail;
import com.aria.entity.UserProfile;
import com.aria.repository.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private final ProductRepository productRepo;
    private final ProductDetailRepository detailRepo;
    private final UserProfileRepository profileRepo;
    private final CartItemRepository cartRepo;
    private final OrderRepository orderRepo;
    private final WishlistRepository wishlistRepo;

    public DebugController(ProductRepository productRepo, ProductDetailRepository detailRepo,
                           UserProfileRepository profileRepo, CartItemRepository cartRepo,
                           OrderRepository orderRepo, WishlistRepository wishlistRepo) {
        this.productRepo = productRepo;
        this.detailRepo = detailRepo;
        this.profileRepo = profileRepo;
        this.cartRepo = cartRepo;
        this.orderRepo = orderRepo;
        this.wishlistRepo = wishlistRepo;
    }

    @GetMapping("/reset")
    public Map<String, Object> resetDatabase() {
        cartRepo.deleteAll();
        orderRepo.deleteAll();
        wishlistRepo.deleteAll();
        detailRepo.deleteAll();
        productRepo.deleteAll();
        profileRepo.deleteAll();

        LocalDate t = LocalDate.now();
        profileRepo.save(new UserProfile("demo@aria.shop", "Иван Петров", "+7 999 123-45-67", "г. Москва, ул. Ленина, д. 1"));

        Product coffee = productRepo.save(new Product("SKU-1001", "Кофе в зёрнах 1 кг", "Продукты", 120, t.minusDays(20), 40, new BigDecimal("350"), new BigDecimal("690"), t.plusDays(300)));
        Product tea = productRepo.save(new Product("SKU-1002", "Чай зелёный 250 г", "Продукты", 80, t.minusDays(35), 22, new BigDecimal("120"), new BigDecimal("260"), t.plusDays(200)));
        Product blanket = productRepo.save(new Product("SKU-2001", "Плед шерстяной", "Дом/текстиль", 60, t.minusDays(210), 0, new BigDecimal("900"), new BigDecimal("1990"), null));
        Product vase = productRepo.save(new Product("SKU-2002", "Ваза керамическая", "Дом/текстиль", 40, t.minusDays(240), 0, new BigDecimal("700"), new BigDecimal("1500"), null));
        Product yogurt = productRepo.save(new Product("SKU-3001", "Йогурт питьевой", "Продукты", 200, t.minusDays(10), 90, new BigDecimal("28"), new BigDecimal("59"), t.plusDays(12)));
        Product gadget = productRepo.save(new Product("SKU-4001", "Гаджет-аксессуар X", "Электроника", 30, t.minusDays(60), 1, new BigDecimal("1200"), new BigDecimal("1100"), null));

        detailRepo.save(new ProductDetail(coffee, "Lavazza", "Brown", "1kg", new BigDecimal("1.0"), "Арабика высшего сорта."));
        detailRepo.save(new ProductDetail(blanket, "WarmHome", "Beige", "150x200cm", new BigDecimal("2.5"), "Шерстяной плед премиум."));
        detailRepo.save(new ProductDetail(yogurt, "Danone", "White", "0.5L", new BigDecimal("0.5"), "Натуральный йогурт."));
        detailRepo.save(new ProductDetail(gadget, "TechPro", "Black", "Universal", new BigDecimal("0.15"), "Адаптер Type-C."));

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "База данных успешно сброшена и заполнена демо-данными.");
        return response;
    }
}