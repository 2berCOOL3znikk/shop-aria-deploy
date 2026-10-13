package com.aria.service;

import com.aria.entity.AriaLog;
import com.aria.entity.Product;
import com.aria.entity.TransferLot;
import com.aria.enums.ProductStatus;
import com.aria.repository.AriaLogRepository;
import com.aria.repository.ProductRepository;
import com.aria.repository.TransferLotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class AriaService {

    private static final int DEAD_STOCK_DAYS = 180;
    private static final int EXPIRY_ALERT_DAYS = 30;

    private final ProductRepository products;
    private final TransferLotRepository lots;
    private final CompetitorAnalyzerService competitorService;
    private final AriaLogRepository logRepo;

    public AriaService(ProductRepository products,
                       TransferLotRepository lots,
                       CompetitorAnalyzerService competitorService,
                       AriaLogRepository logRepo) {
        this.products = products;
        this.lots = lots;
        this.competitorService = competitorService;
        this.logRepo = logRepo;
    }

    @Transactional
    public AnalysisReport analyze() {
        System.out.println("\n[ARIA] === НАЧАЛО АНАЛИЗА === " + LocalDateTime.now());
        LocalDate today = LocalDate.now();
        List<Product> all = products.findAll();

        int toCandidate = 0;
        int toActive = 0;
        int urgent = 0;
        int priceOutliers = 0;

        for (Product p : all) {
            if (p.getStatus() == ProductStatus.TRANSFERRED) continue;

            // Заполняем рыночную цену, если её ещё нет
            if (p.getMarketAvgPrice() == null) {
                BigDecimal marketPrice = competitorService.findMarketAveragePrice(
                        p.getName(), p.getCategory() != null ? p.getCategory() : ""
                );
                p.setMarketAvgPrice(marketPrice);
                System.out.println("[ARIA] " + p.getName() + " -> рыночная цена: " + marketPrice + " руб.");
            }

            List<String> reasons = new ArrayList<>();
            boolean isUrgent = false;

            // Правило 1: Мёртвый сток (>180 дней без продаж)
            long daysInStock = ChronoUnit.DAYS.between(p.getReceivedAt(), today);
            if (daysInStock > DEAD_STOCK_DAYS && p.getSalesLast30Days() == 0) {
                reasons.add("не продаётся " + daysInStock + " дн.");
                System.out.println("[ARIA] " + p.getName() + " - мёртвый сток " + daysInStock + " дн.");
            }

            // Правило 2: Скоропорт (<30 дней до истечения)
            if (p.getExpiryDate() != null) {
                long daysToExpiry = ChronoUnit.DAYS.between(today, p.getExpiryDate());
                if (daysToExpiry < EXPIRY_ALERT_DAYS) {
                    reasons.add("срок годности через " + daysToExpiry + " дн.");
                    isUrgent = true;
                    System.out.println("[ARIA] " + p.getName() + " - скоропорт " + daysToExpiry + " дн.");
                }
            }

            // Правило 3: Отрицательная маржа
            if (p.getUnitCost() != null && p.getUnitPrice() != null) {
                if (p.getUnitPrice().compareTo(p.getUnitCost()) < 0) {
                    reasons.add("отрицательная маржа");
                    System.out.println("[ARIA] " + p.getName() + " - отрицательная маржа");
                }
            }

            // Правило 4: Цена выше рынка на 20%
            if (p.getMarketAvgPrice() != null && p.getUnitPrice() != null) {
                BigDecimal marketPrice = p.getMarketAvgPrice();
                if (marketPrice.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal diffPercent = p.getUnitPrice()
                            .subtract(marketPrice)
                            .divide(marketPrice, 2, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));

                    if (diffPercent.compareTo(new BigDecimal("20")) > 0) {
                        reasons.add("цена выше рынка на " + diffPercent.toPlainString() + "%");
                        priceOutliers++;
                        System.out.println("[ARIA] " + p.getName() + " - выше рынка на " + diffPercent + "%");
                    }
                }
            }

            p.setLastAnalyzedAt(LocalDateTime.now());

            if (!reasons.isEmpty()) {
                if (p.getStatus() != ProductStatus.CANDIDATE && p.getStatus() != ProductStatus.RESERVED) {
                    p.setStatus(ProductStatus.CANDIDATE);
                    toCandidate++;
                }
                if (isUrgent) urgent++;
                p.setAriaReason(String.join("; ", reasons));
            } else {
                if (p.getStatus() == ProductStatus.CANDIDATE) {
                    p.setStatus(ProductStatus.ACTIVE);
                    p.setAriaReason(null);
                    toActive++;
                }
            }
        }

        products.saveAll(all);

        // Сохраняем лог
        AriaLog log = new AriaLog(
                LocalDateTime.now(),
                toCandidate,
                toActive,
                urgent,
                all.size(),
                "Кандидатов: " + toCandidate + ", Вернуто в актив: " + toActive + ", Срочных: " + urgent
        );
        logRepo.save(log);

        System.out.println("[ARIA] Анализ завершён. Кандидатов: " + toCandidate + ", Вернуто в актив: " + toActive);
        System.out.println("[ARIA] === КОНЕЦ АНАЛИЗА ===\n");

        return new AnalysisReport(toCandidate, toActive, urgent, priceOutliers, all.size());
    }

    @Transactional
    public int resetAllStatusesToActive() {
        List<Product> all = products.findAll();
        int resetCount = 0;
        for (Product p : all) {
            if (p.getStatus() == ProductStatus.CANDIDATE || p.getStatus() == ProductStatus.RESERVED) {
                p.setStatus(ProductStatus.ACTIVE);
                p.setAriaReason(null);
                p.setMarketAvgPrice(null);
                resetCount++;
            }
        }
        products.saveAll(all);
        System.out.println("[ARIA] Сброшено статусов: " + resetCount);
        return resetCount;
    }

    @Transactional
    public TransferLot createTransfer(String partner, String note) {
        System.out.println("[ARIA] Создание лота передачи для: " + partner);

        // НЕ вызываем analyze() — кандидаты уже есть после анализа
        List<Product> candidates = products.findByStatus(ProductStatus.CANDIDATE);
        if (candidates.isEmpty()) {
            throw new IllegalStateException("Нет товаров-кандидатов. Сначала запустите анализ ARIA.");
        }

        TransferLot lot = new TransferLot();
        lot.setPartner(partner);
        lot.setNote(note);
        lot.setState("NOTIFIED");

        for (Product p : candidates) {
            p.setStatus(ProductStatus.RESERVED);
            lot.getItems().add(p);
        }

        TransferLot saved = lots.save(lot);
        System.out.println("[ARIA] Лот #" + saved.getId() + " создан с " + candidates.size() + " товарами");
        return saved;
    }

    @Transactional(readOnly = true)
    public String generateActHtml(Long lotId) {
        TransferLot lot = lots.findById(lotId).orElseThrow(() -> new IllegalArgumentException("Лот не найден"));

        String rows = lot.getItems().stream().map(p -> {
            String cost = p.getUnitCost() == null ? "—" : p.getUnitCost().toPlainString();
            String market = p.getMarketAvgPrice() == null ? "—" : p.getMarketAvgPrice().toPlainString();
            String exp = p.getExpiryDate() == null ? "—" : p.getExpiryDate().toString();

            return "<tr><td>" + esc(p.getSku()) + "</td><td>" + esc(p.getName()) + "</td><td>" +
                    esc(p.getCategory()) + "</td><td>" + p.getQuantityOnHand() + "</td><td>" +
                    cost + "</td><td>" + market + "</td><td>" + exp + "</td><td>" +
                    esc(p.getAriaReason()) + "</td></tr>";
        }).collect(java.util.stream.Collectors.joining("\n"));

        int totalQty = lot.getItems().stream().mapToInt(Product::getQuantityOnHand).sum();

        return """
            <!DOCTYPE html>
            <html lang="ru">
            <head><meta charset="UTF-8"><title>Акт приёма-передачи № ARIA-%d</title>
            <style>body{font-family:Arial,sans-serif;padding:20px;} table{border-collapse:collapse;width:100%%;margin-top:20px;} th,td{border:1px solid #ddd;padding:8px;text-align:left;} th{background:#f0f0f0;}</style>
            </head>
            <body>
            <h1>Акт приёма-передачи № ARIA-%d</h1>
            <p><b>Передаёт:</b> ООО «ARIA Shop»</p>
            <p><b>Принимает:</b> %s</p>
            <p><b>Примечание:</b> %s</p>
            <table>
            <tr><th>SKU</th><th>Название</th><th>Категория</th><th>Кол-во</th><th>Себест.</th><th>Рынок</th><th>Срок годн.</th><th>Причина (ARIA)</th></tr>
            %s
            </table>
            <p style="margin-top:20px;"><b>Позиций:</b> %d | <b>Единиц:</b> %d</p>
            <p>Причины зафиксированы автоматически модулем ARIA.</p>
            <br><br>
            <p>Передал: ____________ / ____________ /</p>
            <p>Принял: ____________ / ____________ /</p>
            </body>
            </html>
            """.formatted(lotId, lotId, esc(lot.getPartner()), esc(lot.getNote()), rows, lot.getItems().size(), totalQty);
    }

    private String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public record AnalysisReport(int markedCandidate, int returnedToActive, int urgent, int priceOutliers, int totalScanned) {}
}