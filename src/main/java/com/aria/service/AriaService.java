package com.aria.service;

import com.aria.entity.AriaLog;
import com.aria.entity.DiscountSuggestion;
import com.aria.entity.Product;
import com.aria.entity.TransferLot;
import com.aria.enums.ProductStatus;
import com.aria.repository.AriaLogRepository;
import com.aria.repository.DiscountSuggestionRepository;
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

    private final ProductRepository products;
    private final TransferLotRepository lots;
    private final CompetitorAnalyzerService competitorService;
    private final AriaLogRepository logRepo;
    private final DiscountSuggestionRepository discountRepo;
    private final InventoryService inventory;
    private final PricingService pricing;
    private final PolicyService policies;

    public AriaService(ProductRepository products,
                       TransferLotRepository lots,
                       CompetitorAnalyzerService competitorService,
                       AriaLogRepository logRepo,
                       DiscountSuggestionRepository discountRepo, InventoryService inventory, PricingService pricing,PolicyService policies) {
        this.products = products;
        this.lots = lots;
        this.competitorService = competitorService;
        this.logRepo = logRepo;
        this.discountRepo = discountRepo;
        this.inventory=inventory;this.pricing=pricing;
        this.policies=policies;
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
            if(p.getArchived()) continue;
            var policy=policies.forProduct(p);
            if (p.getStatus() == ProductStatus.TRANSFERRED || p.getStatus() == ProductStatus.WRITTEN_OFF
                    || p.getStatus() == ProductStatus.RESERVED || p.getStatus() == ProductStatus.SOCIAL
                    || p.getStatus() == ProductStatus.DISCOUNTED) continue;

            // Заполняем рыночную цену, если её ещё нет
            if (p.getMarketAvgPrice() == null) {
                BigDecimal marketPrice = competitorService.findMarketAveragePrice(
                        p.getName(), p.getCategory() != null ? p.getCategory() : ""
                );
                p.setMarketAvgPrice(marketPrice);
                    System.out.println("[ARIA] " + p.getName() + " -> ориентир рыночной цены: " + marketPrice + " ₸");
            }

            List<String> reasons = new ArrayList<>();
            boolean isUrgent = false;

            // Правило 1: Мёртвый сток (>180 дней без продаж)
            long daysInStock = ChronoUnit.DAYS.between(p.getReceivedAt(), today);
            if (daysInStock > policy.getSlowStockDays() && p.getSalesLast30Days() == 0) {
                reasons.add("не продаётся " + daysInStock + " дн.");
                System.out.println("[ARIA] " + p.getName() + " - мёртвый сток " + daysInStock + " дн.");
            }

            // Правило 2: Скоропорт (<30 дней до истечения)
            if (p.getExpiryDate() != null) {
                long daysToExpiry = ChronoUnit.DAYS.between(today, p.getExpiryDate());
                if (daysToExpiry < 0) {
                    reasons.add("срок годности истёк " + Math.abs(daysToExpiry) + " дн. назад — передача запрещена");
                    System.out.println("[ARIA] " + p.getName() + " - срок годности истёк");
                } else if (daysToExpiry <= policy.getWarningDays()) {
                    reasons.add("срок годности через " + daysToExpiry + " дн.");
                    isUrgent = daysToExpiry <= policy.getCriticalDays();
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
            if (p.getStatus() == ProductStatus.CANDIDATE) {
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
    public Product writeOff(Long productId) {
        Product product = inventory.lock(productId);
        if (product.getStatus() == ProductStatus.RESERVED || product.getStatus() == ProductStatus.TRANSFERRED) {
            throw new IllegalStateException("Зарезервированный или переданный товар нельзя списать повторно");
        }
        inventory.writeOffAll(product,"Списание из панели анализа");
        product.setStatus(ProductStatus.WRITTEN_OFF);
        product.setQuantityOnHand(0);
        product.setAriaReason("Списан оператором " + LocalDate.now());
        return products.save(product);
    }

    @Transactional
    public TransferLot createTransfer(String partner, String note) {
        return createTransfer(partner, note, null);
    }

    @Transactional
    public TransferLot createTransfer(String partner, String note, List<Long> selectedProductIds) {
        System.out.println("[ARIA] Создание лота передачи для: " + partner);

        // Передавать можно только выбранных кандидатов с действующим сроком годности.
        List<Product> candidates = products.findByStatus(ProductStatus.CANDIDATE).stream()
                .filter(p -> p.getExpiryDate() == null || !p.getExpiryDate().isBefore(LocalDate.now()))
                .filter(p -> selectedProductIds == null || selectedProductIds.contains(p.getId()))
                .toList();
        if (candidates.isEmpty()) {
            throw new IllegalStateException("Не выбраны подходящие товары: проверьте, что они кандидаты и срок годности не истёк.");
        }

        TransferLot lot = new TransferLot();
        lot.setPartner(partner == null || partner.isBlank() ? "Благотворительная организация" : partner.trim());
        lot.setNote(note);
        lot.setState("DRAFT");

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
            BigDecimal cost = p.getUnitCost() == null ? BigDecimal.ZERO : p.getUnitCost();
            int quantity = p.getQuantityOnHand() == null ? 0 : p.getQuantityOnHand();
            String exp = p.getExpiryDate() == null ? "—" : p.getExpiryDate().toString();

            return "<tr><td>" + esc(p.getSku()) + "</td><td>" + esc(p.getName()) + "<br><small>" + esc(p.getCategory()) + "</small></td><td>" +
                    quantity + "</td><td>" + cost.toPlainString() + "</td><td>" + cost.multiply(BigDecimal.valueOf(quantity)).toPlainString() + "</td><td>" + exp + "</td><td>" +
                    esc(p.getAriaReason()) + "</td></tr>";
        }).collect(java.util.stream.Collectors.joining("\n"));

        int totalQty = lot.getItems().stream().mapToInt(p -> p.getQuantityOnHand() == null ? 0 : p.getQuantityOnHand()).sum();

        return """
            <!DOCTYPE html>
            <html lang="ru">
            <head><meta charset="UTF-8"><title>Акт приёма-передачи № ARIA-%d</title>
            <style>body{font-family:Arial,sans-serif;padding:20px;} table{border-collapse:collapse;width:100%%;margin-top:20px;} th,td{border:1px solid #ddd;padding:8px;text-align:left;} th{background:#f0f0f0;}</style>
            </head>
            <body>
            <h1>Акт приёма-передачи № ARIA-%d</h1>
            <p><b>Дата формирования:</b> %s</p>
            <p><b>Передающая сторона:</b> ARIA Shop</p>
            <p><b>Принимающая организация:</b> %s</p>
            <p><b>Основание / примечание:</b> %s</p>
            <table>
            <tr><th>SKU</th><th>Товар и категория</th><th>Кол-во</th><th>Себестоимость за ед., ₸</th><th>Стоимость партии, ₸</th><th>Срок годности</th><th>Основание ARIA</th></tr>
            %s
            </table>
            <p style="margin-top:20px;"><b>Позиций:</b> %d | <b>Единиц:</b> %d | <b>Балансовая стоимость партии:</b> %s ₸</p>
            <p>Черновик документа: проверьте номенклатуру, количество и реквизиты сторон перед подписанием.</p>
            <br><br>
            <p>Передал: ____________________ &nbsp; М.П.</p>
            <p>Принял: ____________________ &nbsp; М.П.</p>
            <p>Документ сформирован ARIA Shop. Налоговый и бухгалтерский учёт оформляется ответственными сторонами по применимым правилам.</p>
            </body>
            </html>
            """.formatted(lotId, lotId, lot.getCreatedAt().toLocalDate(), esc(lot.getPartner()), esc(lot.getNote()), rows, lot.getItems().size(), totalQty,
                lot.getItems().stream().map(p -> (p.getUnitCost() == null ? BigDecimal.ZERO : p.getUnitCost())
                        .multiply(BigDecimal.valueOf(p.getQuantityOnHand() == null ? 0 : p.getQuantityOnHand())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP).toPlainString());
    }

    private String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @Transactional(readOnly = true)
    public List<TransferLot> listTransfers() {
        return lots.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }

    /**
     * Предлагает скидки для товаров-кандидатов со сроком годности.
     * Тиры скидок по оставшимся дням:
     * - <=1 день: 50% (priority clearance)
     * - <=7 дней: 30%
     * - <=30 дней: 15%
     * - >30 дней: без предложения
     * Пропускает товары, для которых уже есть PENDING предложение.
     */
    @Transactional
    public int proposeDiscounts() {
        System.out.println("[ARIA] === НАЧАЛО ПРЕДЛОЖЕНИЙ СКИДОК === " + LocalDateTime.now());
        LocalDate today = LocalDate.now();
        List<Product> candidates = products.findByStatus(ProductStatus.CANDIDATE);
        int suggestionsCreated = 0;

        for (Product p : candidates) {
            // Пропускаем, если уже есть PENDING предложение для этого товара
            if (discountRepo.existsByProductIdAndStatus(p.getId(), "PENDING")) {
                System.out.println("[ARIA] " + p.getName() + " - уже есть PENDING предложение, пропускаем");
                continue;
            }

            // Вычисляем оставшиеся дни
            long daysToExpiry = p.getExpiryDate()==null?Long.MAX_VALUE:ChronoUnit.DAYS.between(today, p.getExpiryDate());
            if(daysToExpiry<0 || p.getArchived() || p.getQuantityOnHand()==null || p.getQuantityOnHand()<1) continue;

            var recommendation=pricing.recommendation(p);
            int discountPercent=((Number)recommendation.get("suggestedPercent")).intValue();
            String reason=(String)recommendation.get("reason");
            if(discountPercent<1) continue;

            // Создаём предложение (каждое в try/catch для изоляции ошибок)
            try {
                discountPercent=Math.min(discountPercent,pricing.maximum(p));
                if(discountPercent<1) continue;
                DiscountSuggestion suggestion = new DiscountSuggestion(p, reason+"; ограничение по себестоимости учтено", discountPercent);
                discountRepo.save(suggestion);
                suggestionsCreated++;
                System.out.println("[ARIA] " + p.getName() + " - предложена скидка " + discountPercent + "% (" + reason + ")");
            } catch (Exception e) {
                System.err.println("[ARIA] Ошибка при создании предложения для " + p.getName() + ": " + e.getMessage());
            }
        }

        System.out.println("[ARIA] Предложений создано: " + suggestionsCreated);
        System.out.println("[ARIA] === КОНЕЦ ПРЕДЛОЖЕНИЙ СКИДОК ===\n");
        return suggestionsCreated;
    }

    /**
     * Одобряет предложение о скидке: применяет скидку к товару.
     * Устанавливает product.status = DISCOUNTED и product.discountPercent = percent.
     * НЕ изменяет unitPrice, чтобы сохранить обратимость.
     */
    @Transactional
    public void approveDiscount(Long suggestionId, String userEmail) {
        DiscountSuggestion suggestion = discountRepo.findById(suggestionId)
                .orElseThrow(() -> new IllegalArgumentException("Предложение не найдено"));

        if (!"PENDING".equals(suggestion.getStatus())) {
            throw new IllegalStateException("Предложение уже обработано: " + suggestion.getStatus());
        }

        Product product = suggestion.getProduct();
        product=inventory.lock(product.getId());
        inventory.synchronize(product);
        pricing.validate(product,suggestion.getSuggestedPercent());
        product.setStatus(ProductStatus.DISCOUNTED);
        product.setDiscountPercent(suggestion.getSuggestedPercent());

        suggestion.setStatus("APPROVED");
        suggestion.setDecidedBy(userEmail);
        suggestion.setDecidedAt(LocalDateTime.now());

        discountRepo.save(suggestion);
        products.save(product);

        System.out.println("[ARIA] Предложение #" + suggestionId + " одобрено пользователем " + userEmail +
                ". Товар " + product.getName() + " теперь со скидкой " + suggestion.getSuggestedPercent() + "%");
    }

    /**
     * Отклоняет предложение о скидке.
     */
    @Transactional
    public void rejectDiscount(Long suggestionId, String userEmail) {
        DiscountSuggestion suggestion = discountRepo.findById(suggestionId)
                .orElseThrow(() -> new IllegalArgumentException("Предложение не найдено"));

        if (!"PENDING".equals(suggestion.getStatus())) {
            throw new IllegalStateException("Предложение уже обработано: " + suggestion.getStatus());
        }

        suggestion.setStatus("REJECTED");
        suggestion.setDecidedBy(userEmail);
        suggestion.setDecidedAt(LocalDateTime.now());

        discountRepo.save(suggestion);

        System.out.println("[ARIA] Предложение #" + suggestionId + " отклонено пользователем " + userEmail);
    }

    public record AnalysisReport(int markedCandidate, int returnedToActive, int urgent, int priceOutliers, int totalScanned) {}
}
