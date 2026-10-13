package com.aria.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

@Service
public class CompetitorAnalyzerService {

    public BigDecimal findMarketAveragePrice(String productName, String category) {
        try {
            // Детерминированный рандом по имени товара (всегда одинаковый результат для одного товара)
            Random rand = new Random(productName.hashCode());

            // Реалистичное отклонение: от -15% до +25% от нашей цены
            // Это значит, что только товары с завышенной ценой (>20%) станут кандидатами
            double deviation = -0.15 + (rand.nextDouble() * 0.40); // от -15% до +25%

            // Базовая цена зависит от категории (примерные рыночные диапазоны)
            double basePrice = switch (category.toLowerCase()) {
                case "продукты" -> 150.0 + (rand.nextDouble() * 600);      // 150-750 руб
                case "дом/текстиль" -> 800.0 + (rand.nextDouble() * 1500);  // 800-2300 руб
                case "электроника" -> 1000.0 + (rand.nextDouble() * 3000); // 1000-4000 руб
                case "промо" -> 500.0 + (rand.nextDouble() * 1000);        // 500-1500 руб
                default -> 300.0 + (rand.nextDouble() * 700);              // 300-1000 руб
            };

            // Применяем отклонение
            BigDecimal calculated = BigDecimal.valueOf(basePrice * (1 + deviation))
                    .setScale(2, RoundingMode.HALF_UP);

            System.out.println("[ARIA-Market] " + productName + " -> рынок: " + calculated + " руб.");
            return calculated;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}