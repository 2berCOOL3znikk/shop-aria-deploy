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
    private final com.aria.service.InventoryService inventory;
    private final com.aria.service.PublicProductView view;

    public ProductController(ProductRepository repo, ProductDetailRepository detailRepo, com.aria.service.InventoryService inventory,com.aria.service.PublicProductView view) {
        this.repo = repo;
        this.detailRepo = detailRepo;
        this.inventory=inventory; this.view=view;
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
    private Map<String, Object> productToMap(Product product) { return view.summary(product); }

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
        return products.stream().filter(p -> !p.getArchived() && p.getUnitPrice()!=null && p.getUnitPrice().signum()>0 && inventory.available(p)>0).toList();
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
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Товар не найден"));
        if ((product.getStatus() != ProductStatus.ACTIVE && product.getStatus() != ProductStatus.DISCOUNTED)
                || product.getArchived() || inventory.available(product)<1) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Товар не найден в каталоге");
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
                    .filter(p -> !p.getArchived() && inventory.available(p)>0)
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
