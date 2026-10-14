package com.aria.controller;

import com.aria.entity.DiscountSuggestion;
import com.aria.entity.TransferLot;
import com.aria.repository.DiscountSuggestionRepository;
import com.aria.service.AriaService;
import com.aria.service.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/aria")
public class AriaController {

    private final AriaService aria;
    private final StatsService statsService;
    private final DiscountSuggestionRepository discountRepo;

    public AriaController(AriaService aria, StatsService statsService, DiscountSuggestionRepository discountRepo) {
        this.aria = aria;
        this.statsService = statsService;
        this.discountRepo = discountRepo;
    }

    @PostMapping("/analyze")
    public AriaService.AnalysisReport analyze() {
        return aria.analyze();
    }

    @PostMapping("/propose-discounts")
    public Map<String, Object> proposeDiscounts() {
        int count = aria.proposeDiscounts();
        Map<String, Object> res = new HashMap<>();
        res.put("suggestionsCreated", count);
        res.put("message", "Создано " + count + " предложений о скидках для товаров-кандидатов");
        return res;
    }

    @PostMapping("/reset-statuses")
    public Map<String, Object> resetStatuses() {
        int count = aria.resetAllStatusesToActive();
        Map<String, Object> res = new HashMap<>();
        res.put("resetCount", count);
        res.put("message", "Статусы " + count + " товаров сброшены в АКТИВЕН. Теперь можно запустить анализ заново.");
        return res;
    }

    @PostMapping("/products/{id}/write-off")
    public Map<String, Object> writeOff(@PathVariable Long id) {
        var product = aria.writeOff(id);
        return Map.of("message", "Товар списан", "productId", product.getId(), "status", product.getStatus().name());
    }

    @PostMapping("/transfer")
    public TransferLot transfer(
            @RequestParam(defaultValue = "Фонд Тёплый дом") String partner,
            @RequestParam(required = false) String note,
            @RequestParam(required = false) List<Long> productIds) {
        return aria.createTransfer(partner, note, productIds);
    }

    @GetMapping("/act/{lotId}")
    public ResponseEntity<String> act(@PathVariable Long lotId) {
        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=\"aria-act-" + lotId + ".html\"")
                .body(aria.generateActHtml(lotId));
    }

    @GetMapping("/lots")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getLots() {
        return aria.listTransfers().stream().map(lot -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", lot.getId());
            row.put("partner", lot.getPartner());
            row.put("state", lot.getState());
            row.put("createdAt", lot.getCreatedAt());
            row.put("itemCount", lot.getItems().size());
            row.put("quantity", lot.getItems().stream().mapToInt(p -> p.getQuantityOnHand() == null ? 0 : p.getQuantityOnHand()).sum());
            row.put("actUrl", "/api/aria/act/" + lot.getId());
            return row;
        }).toList();
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return statsService.getEconomicReport();
    }

    // === Эндпоинты для предложений о скидках ===

    /**
     * Получить список предложений (PENDING + последние APPROVED/REJECTED).
     * Возвращает скалярные карты для избежания lazy-прокси.
     */
    @GetMapping("/suggestions")
    public List<Map<String, Object>> getSuggestions() {
        List<DiscountSuggestion> pending = discountRepo.findByStatusOrderByProposedAtDesc("PENDING");
        List<DiscountSuggestion> approved = discountRepo.findByStatusOrderByProposedAtDesc("APPROVED");
        List<DiscountSuggestion> rejected = discountRepo.findByStatusOrderByProposedAtDesc("REJECTED");

        List<DiscountSuggestion> recentApproved = approved.isEmpty() ? approved : approved.subList(0, Math.min(10, approved.size()));
        List<DiscountSuggestion> recentRejected = rejected.isEmpty() ? rejected : rejected.subList(0, Math.min(10, rejected.size()));

        List<DiscountSuggestion> all = new java.util.ArrayList<>();
        all.addAll(pending);
        all.addAll(recentApproved);
        all.addAll(recentRejected);

        return all.stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getId());
            map.put("productId", s.getProduct().getId());
            map.put("productName", s.getProduct().getName());
            map.put("productSku", s.getProduct().getSku());
            map.put("reason", s.getReason());
            map.put("suggestedPercent", s.getSuggestedPercent());
            map.put("proposedAt", s.getProposedAt().toString());
            map.put("status", s.getStatus());
            map.put("decidedBy", s.getDecidedBy());
            map.put("decidedAt", s.getDecidedAt() != null ? s.getDecidedAt().toString() : null);
            return map;
        }).toList();
    }

    /**
     * Одобрить предложение о скидке (требует роль ADMIN).
     * Применяет скидку к товару: статус = DISCOUNTED, discountPercent = percent.
     */
    @PostMapping("/suggestions/{id}/approve")
    public Map<String, Object> approveSuggestion(@PathVariable Long id, Authentication authentication) {
        String userEmail = authentication.getName();
        aria.approveDiscount(id, userEmail);
        Map<String, Object> res = new HashMap<>();
        res.put("message", "Скидка одобрена и применена к товару");
        return res;
    }

    /**
     * Отклонить предложение о скидке (требует роль ADMIN).
     */
    @PostMapping("/suggestions/{id}/reject")
    public Map<String, Object> rejectSuggestion(@PathVariable Long id, Authentication authentication) {
        String userEmail = authentication.getName();
        aria.rejectDiscount(id, userEmail);
        Map<String, Object> res = new HashMap<>();
        res.put("message", "Предложение отклонено");
        return res;
    }
}
