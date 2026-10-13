package com.aria.controller;

import com.aria.entity.Product;
import com.aria.repository.ProductRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stats")
public class CategoryStatsController {

    private final ProductRepository productRepo;

    public CategoryStatsController(ProductRepository productRepo) {
        this.productRepo = productRepo;
    }

    @GetMapping("/categories")
    public Map<String, Object> getCategoryStats() {
        List<Product> all = productRepo.findAll();

        // Группировка по категориям
        Map<String, Long> byCategory = all.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getCategory() != null ? p.getCategory() : "Без категории",
                        Collectors.counting()
                ));

        // Статистика по статусам
        Map<String, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getStatus().name(),
                        Collectors.counting()
                ));

        // Общие цифры
        long totalProducts = all.size();
        long activeProducts = all.stream().filter(p -> p.getStatus().name().equals("ACTIVE")).count();
        long candidateProducts = all.stream().filter(p -> p.getStatus().name().equals("CANDIDATE")).count();

        Map<String, Object> result = new HashMap<>();
        result.put("totalProducts", totalProducts);
        result.put("activeProducts", activeProducts);
        result.put("candidateProducts", candidateProducts);
        result.put("byCategory", byCategory);
        result.put("byStatus", byStatus);

        return result;
    }

    @GetMapping("/recent")
    public List<Product> getRecentProducts() {
        // Последние 5 добавленных товаров
        return productRepo.findAll().stream()
                .sorted((a, b) -> b.getReceivedAt().compareTo(a.getReceivedAt()))
                .limit(5)
                .collect(Collectors.toList());
    }
}