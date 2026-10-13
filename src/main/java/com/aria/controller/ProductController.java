package com.aria.controller;

import com.aria.entity.Product;
import com.aria.entity.ProductDetail;
import com.aria.enums.ProductStatus;
import com.aria.repository.ProductDetailRepository;
import com.aria.repository.ProductRepository;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository repo;
    private final ProductDetailRepository detailRepo;

    public ProductController(ProductRepository repo, ProductDetailRepository detailRepo) {
        this.repo = repo;
        this.detailRepo = detailRepo;
    }

    @GetMapping
    public List<Product> all() {
        return repo.findAll();
    }

    @GetMapping("/store")
    public List<Product> store() {
        return repo.findByStatus(ProductStatus.ACTIVE);
    }

    @GetMapping("/social")
    public List<Product> social() {
        List<Product> res = new ArrayList<>(repo.findByStatus(ProductStatus.CANDIDATE));
        res.addAll(repo.findByStatus(ProductStatus.RESERVED));
        return res;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        Product product = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден: " + id));

        ProductDetail details = detailRepo.findByProductId(id).orElse(null);

        Map<String, Object> productMap = new HashMap<>();
        productMap.put("id", product.getId());
        productMap.put("sku", product.getSku());
        productMap.put("name", product.getName());
        productMap.put("category", product.getCategory());
        productMap.put("quantityOnHand", product.getQuantityOnHand());
        productMap.put("receivedAt", product.getReceivedAt());
        productMap.put("salesLast30Days", product.getSalesLast30Days());
        productMap.put("unitCost", product.getUnitCost());
        productMap.put("unitPrice", product.getUnitPrice());
        productMap.put("expiryDate", product.getExpiryDate());
        productMap.put("status", product.getStatus() != null ? product.getStatus().name() : null);
        productMap.put("ariaReason", product.getAriaReason());
        productMap.put("marketAvgPrice", product.getMarketAvgPrice());
        productMap.put("lastAnalyzedAt", product.getLastAnalyzedAt());

        Map<String, Object> detailsMap = null;
        if (details != null) {
            detailsMap = new HashMap<>();
            detailsMap.put("brand", details.getBrand());
            detailsMap.put("color", details.getColor());
            detailsMap.put("size", details.getSize());
            detailsMap.put("weightKg", details.getWeightKg());
            detailsMap.put("descriptionLong", details.getDescriptionLong());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("product", productMap);
        result.put("details", detailsMap);
        return result;
    }

    @GetMapping("/search")
    public List<Product> search(@RequestParam String q) {
        if (q == null || q.trim().isEmpty()) {
            return repo.findByStatus(ProductStatus.ACTIVE);
        }
        return repo.searchProducts(q.trim());
    }

    @GetMapping("/{id}/image")
    public Map<String, String> getProductImage(@PathVariable Long id) {
        Product product = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

        // Генерируем красивую заглушку с названием товара
        String text = product.getName().replace(" ", "+");
        String colors = "1e293b/3b82f6"; // тёмный фон, синяя надпись

        Map<String, String> result = new HashMap<>();
        result.put("imageUrl", "https://placehold.co/400x300/" + colors + "?text=" + text);
        return result;
    }

    @GetMapping("/filter")
    public List<Product> filterProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "name") String sortBy) {

        List<Product> products = repo.findAll();

        // Фильтр по категории
        if (category != null && !category.isEmpty()) {
            products = products.stream()
                    .filter(p -> category.equals(p.getCategory()))
                    .collect(Collectors.toList());
        }

        // Фильтр по статусу
        if (status != null && !status.isEmpty()) {
            products = products.stream()
                    .filter(p -> status.equals(p.getStatus().name()))
                    .collect(Collectors.toList());
        }

        // Сортировка
        switch (sortBy) {
            case "price-asc":
                products.sort((a, b) -> a.getUnitPrice().compareTo(b.getUnitPrice()));
                break;
            case "price-desc":
                products.sort((a, b) -> b.getUnitPrice().compareTo(a.getUnitPrice()));
                break;
            case "popular":
                products.sort((a, b) -> b.getSalesLast30Days().compareTo(a.getSalesLast30Days()));
                break;
            default:
                products.sort((a, b) -> a.getName().compareTo(b.getName()));
        }

        return products;
    }

}