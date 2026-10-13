package com.aria.controller;

import com.aria.entity.Product;
import com.aria.service.FavoriteService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {
    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public List<Product> getFavorites() {
        return favoriteService.getAllFavorites();
    }

    @PostMapping("/{productId}")
    public Map<String, Object> toggle(@PathVariable Long productId) {
        boolean wasFavorite = favoriteService.isFavorite(productId);
        if (wasFavorite) {
            favoriteService.removeFavorite(productId);
        } else {
            favoriteService.addFavorite(productId);
        }
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("isFavorite", !wasFavorite);
        return res;
    }

    @DeleteMapping("/{productId}")
    public Map<String, Object> remove(@PathVariable Long productId) {
        favoriteService.removeFavorite(productId);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        return res;
    }

    @GetMapping("/check/{productId}")
    public Map<String, Boolean> check(@PathVariable Long productId) {
        Map<String, Boolean> res = new HashMap<>();
        res.put("isFavorite", favoriteService.isFavorite(productId));
        return res;
    }

    @GetMapping("/ids")
    public Map<String, Object> ids() {
        List<Product> favs = favoriteService.getAllFavorites();
        List<Long> ids = favs.stream().map(Product::getId).toList();
        Map<String, Object> res = new HashMap<>();
        res.put("ids", ids);
        return res;
    }
}