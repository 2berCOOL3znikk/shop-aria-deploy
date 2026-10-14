package com.aria.controller;

import com.aria.entity.Product;
import com.aria.entity.ProductDetail;
import com.aria.enums.ProductStatus;
import com.aria.repository.ProductDetailRepository;
import com.aria.repository.ProductRepository;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    /**
     * Вычисляет отображаемую цену с учётом скидки.
     * displayPrice = unitPrice * (1 - discountPercent / 100)
     */
    private BigDecimal computeDisplayPrice(Product product) {
        if (product.getDiscountPercent() == null || product.getDiscountPercent() == 0) {
            return product.getUnitPrice();
        }
        BigDecimal discount = BigDecimal.valueOf(product.getDiscountPercent())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return product.getUnitPrice()
                .multiply(BigDecimal.ONE.subtract(discount))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Публичное представление товара без закупочной себестоимости и внутренних метрик склада.
     */
    private Map<String, Object> productToMap(Product product) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", product.getId());
        map.put("sku", product.getSku());
        map.put("name", product.getName());
        map.put("imageUrl", product.getImageUrl() != null ? product.getImageUrl() : "/images/product-generic.svg");
        map.put("category", product.getCategory());
        map.put("quantityOnHand", product.getQuantityOnHand());
        map.put("unitPrice", product.getUnitPrice());
        map.put("discountPercent", product.getDiscountPercent());
        map.put("displayPrice", computeDisplayPrice(product));
        map.put("expiryDate", product.getExpiryDate());
        if (product.getStatus() == ProductStatus.ACTIVE || product.getStatus() == ProductStatus.DISCOUNTED) {
            map.put("status", product.getStatus().name());
        }
        int sales = product.getSalesLast30Days() == null ? 0 : product.getSalesLast30Days();
        map.put("popularityRank", sales >= 50 ? 3 : sales >= 20 ? 2 : sales > 0 ? 1 : 0);
        if (product.getMarketAvgPrice() != null && product.getMarketAvgPrice().compareTo(BigDecimal.ZERO) > 0
                && product.getUnitPrice() != null && product.getUnitPrice().compareTo(product.getMarketAvgPrice()) < 0) {
            BigDecimal dealPercent = product.getMarketAvgPrice().subtract(product.getUnitPrice())
                    .multiply(BigDecimal.valueOf(100)).divide(product.getMarketAvgPrice(), 0, RoundingMode.HALF_UP);
            map.put("marketDealPercent", dealPercent.intValue());
        }
        detailRepo.findByProductId(product.getId()).ifPresent(detail -> {
            map.put("brand", detail.getBrand());
            map.put("description", detail.getDescriptionLong());
            map.put("size", detail.getSize());
        });
        return map;
    }

    @GetMapping
    public List<Product> all() {
        return repo.findAll();
    }

    @GetMapping("/store")
    public List<Map<String, Object>> store() {
        return sellableProducts().stream().map(this::productToMap).toList();
    }

    private List<Product> sellableProducts() {
        List<Product> products = new ArrayList<>(repo.findByStatus(ProductStatus.ACTIVE));
        products.addAll(repo.findByStatus(ProductStatus.DISCOUNTED));
        return products.stream().filter(p -> p.getQuantityOnHand() != null && p.getQuantityOnHand() > 0).toList();
    }

    @GetMapping("/social")
    public List<Map<String, Object>> social() {
        List<Product> res = new ArrayList<>(repo.findByStatus(ProductStatus.CANDIDATE));
        res.addAll(repo.findByStatus(ProductStatus.RESERVED));
        return res.stream().map(this::productToMap).toList();
    }

    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        Product product = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден: " + id));
        if ((product.getStatus() != ProductStatus.ACTIVE && product.getStatus() != ProductStatus.DISCOUNTED)
                || product.getQuantityOnHand() == null || product.getQuantityOnHand() < 1) {
            throw new IllegalArgumentException("Товар не найден в каталоге");
        }

        ProductDetail details = detailRepo.findByProductId(id).orElse(null);

        Map<String, Object> productMap = productToMap(product);

        Map<String, Object> detailsMap = null;
        if (details != null) {
            detailsMap = new HashMap<>();
            detailsMap.put("brand", details.getBrand());
            detailsMap.put("color", details.getColor());
            detailsMap.put("size", details.getSize());
            detailsMap.put("weightKg", details.getWeightKg());
            detailsMap.put("descriptionLong", details.getDescriptionLong());
            detailsMap.put("imageUrl", product.getImageUrl());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("product", productMap);
        result.put("details", detailsMap);
        return result;
    }

    @GetMapping("/search")
    public List<Map<String, Object>> search(@RequestParam String q) {
        List<Product> products;
        if (q == null || q.trim().isEmpty()) {
            products = sellableProducts();
        } else {
            products = repo.searchProducts(q.trim()).stream()
                    .filter(p -> p.getStatus() == ProductStatus.ACTIVE || p.getStatus() == ProductStatus.DISCOUNTED)
                    .filter(p -> p.getQuantityOnHand() != null && p.getQuantityOnHand() > 0)
                    .toList();
        }
        return products.stream().map(this::productToMap).toList();
    }

    @GetMapping("/{id}/image")
    public Map<String, String> getProductImage(@PathVariable Long id) {
        Product product = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

        Map<String, String> result = new HashMap<>();
        result.put("imageUrl", product.getImageUrl() != null ? product.getImageUrl() : "/images/product-generic.svg");
        return result;
    }

    @GetMapping("/filter")
    public List<Map<String, Object>> filterProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "name") String sortBy) {

        List<Product> products = new ArrayList<>(sellableProducts());

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

        return products.stream().map(this::productToMap).toList();
    }

}
