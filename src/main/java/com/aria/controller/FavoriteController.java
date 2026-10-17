package com.aria.controller;

import com.aria.entity.Product;
import com.aria.service.FavoriteServicebak;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {
    private final FavoriteServicebak favoriteService;

    public FavoriteController(FavoriteServicebak favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public List<Product> getFavorites(Authentication authentication) {
        return favoriteService.getAllFavorites(authentication.getName());
    }

    @PostMapping("/{productId}")
    public Map<String, Object> toggle(@PathVariable Long productId, Authentication authentication) {
        String ownerEmail = authentication.getName();
        boolean wasFavorite = favoriteService.isFavorite(productId, ownerEmail);
        if (wasFavorite) {
            favoriteService.removeFavorite(productId, ownerEmail);
        } else {
            favoriteService.addFavorite(productId, ownerEmail);
        }
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("isFavorite", !wasFavorite);
        return res;
    }

    @DeleteMapping("/{productId}")
    public Map<String, Object> remove(@PathVariable Long productId, Authentication authentication) {
        favoriteService.removeFavorite(productId, authentication.getName());
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        return res;
    }

    @GetMapping("/check/{productId}")
    public Map<String, Boolean> check(@PathVariable Long productId, Authentication authentication) {
        Map<String, Boolean> res = new HashMap<>();
        res.put("isFavorite", favoriteService.isFavorite(productId, authentication.getName()));
        return res;
    }

    @GetMapping("/ids")
    public Map<String, Object> ids(Authentication authentication) {
        List<Product> favs = favoriteService.getAllFavorites(authentication.getName());
        List<Long> ids = favs.stream().map(Product::getId).toList();
        Map<String, Object> res = new HashMap<>();
        res.put("ids", ids);
        return res;
    }
}
