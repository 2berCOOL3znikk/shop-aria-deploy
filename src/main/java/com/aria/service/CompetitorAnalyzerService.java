package com.aria.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

@Service
public class CompetitorAnalyzerService {

    @org.springframework.beans.factory.annotation.Value("${aria.demo-market-enabled:false}")
    private boolean demoMarketEnabled;

    public BigDecimal findMarketAveragePrice(String productName, String category) {
        // No external market feed is connected. Never invent a comparison in normal operation.
        if (!demoMarketEnabled) return null;
        try {
            // Детерминированный рандом по имени товара (всегда одинаковый результат для одного товара)
            Random rand = new Random(productName.hashCode());

            // Реалистичное отклонение: от -15% до +25% от нашей цены
            // Это значит, что только товары с завышенной ценой (>20%) станут кандидатами
            double deviation = -0.15 + (rand.nextDouble() * 0.40); // от -15% до +25%

            // Базовая цена зависит от категории (примерные рыночные диапазоны)
            double basePrice = switch (category.toLowerCase()) {
                case "продукты" -> (150.0 + (rand.nextDouble() * 600)) * 5.36;
                case "дом/текстиль" -> (800.0 + (rand.nextDouble() * 1500)) * 5.36;
                case "электроника" -> (1000.0 + (rand.nextDouble() * 3000)) * 5.36;
                case "промо" -> (500.0 + (rand.nextDouble() * 1000)) * 5.36;
                default -> (300.0 + (rand.nextDouble() * 700)) * 5.36;
            };

            // Применяем отклонение
            BigDecimal calculated = BigDecimal.valueOf(basePrice * (1 + deviation))
                    .setScale(2, RoundingMode.HALF_UP);

            System.out.println("[ARIA-Market] " + productName + " -> рыночный ориентир: " + calculated + " ₸");
            return calculated;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
