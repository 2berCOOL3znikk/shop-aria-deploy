package com.aria.service;

import com.aria.entity.Product;
import com.aria.enums.ProductStatus;
import com.aria.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StatsService {

    private final ProductRepository products;

    public StatsService(ProductRepository products) {
        this.products = products;
    }

    public Map<String, Object> getEconomicReport() {
        List<Product> all = products.findAll();

        BigDecimal deadStockValue = all.stream()
                .filter(p -> p.getStatus() == ProductStatus.CANDIDATE)
                .map(p -> {
                    if (p.getUnitCost() == null || p.getQuantityOnHand() == null) return BigDecimal.ZERO;
                    return p.getUnitCost().multiply(BigDecimal.valueOf(p.getQuantityOnHand()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long savedCount = all.stream()
                .filter(p -> p.getStatus() == ProductStatus.RESERVED || p.getStatus() == ProductStatus.TRANSFERRED)
                .count();

        BigDecimal taxBenefitEstimate = deadStockValue.multiply(new BigDecimal("0.20"));

        Map<String, Long> categoryBreakdown = all.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getCategory() != null ? p.getCategory() : "Без категории",
                        Collectors.counting()
                ));

        return Map.of(
                "deadStockValue", deadStockValue,
                "savedItemsCount", savedCount,
                "taxBenefitEstimate", taxBenefitEstimate,
                "categoryBreakdown", categoryBreakdown,
                "totalProducts", all.size(),
                "activeProducts", all.stream().filter(p -> p.getStatus() == ProductStatus.ACTIVE).count()
        );
    }
}