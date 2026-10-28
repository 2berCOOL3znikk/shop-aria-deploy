package com.aria.controller;

import com.aria.entity.SocialOffer;
import com.aria.entity.AppUser;
import com.aria.repository.AppUserRepository;
import com.aria.service.SocialService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/social")
public class SocialController {

    private final SocialService socialService;
    private final AppUserRepository users;

    public SocialController(SocialService socialService, AppUserRepository users) {
        this.socialService = socialService;
        this.users = users;
    }

    /**
     * Получить список открытых социальных предложений (permitAll).
     * Возвращает скалярные карты БЕЗ orgId для сохранения приватности.
     */
    @GetMapping("/offers")
    public List<Map<String, Object>> getOffers() {
        List<SocialOffer> offers = socialService.listOpenOffers();
        return offers.stream().map(offer -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", offer.getId());
            map.put("productId", offer.getProduct().getId());
            map.put("productName", offer.getProduct().getName());
            map.put("productSku", offer.getProduct().getSku());
            map.put("socialPrice", offer.getSocialPrice());
            map.put("quantityAvailable", offer.getQuantityAvailable());
            map.put("validUntil", offer.getValidUntil() == null ? null : offer.getValidUntil().toString());
            map.put("expiryDate", offer.getProduct().getExpiryDate());
            map.put("ariaReason", publicReason(offer.getProduct().getAriaReason()));
            map.put("imageUrl", offer.getProduct().getImageUrl());
            map.put("status", offer.getStatus());
            map.put("createdAt", offer.getCreatedAt().toString());
            // orgId НЕ включается для приватности
            return map;
        }).toList();
    }

    @GetMapping("/impact")
    public Map<String, Long> getImpact() {
        return socialService.getPublicImpact();
    }

    @PostMapping("/offers")
    public ResponseEntity<?> publishOffer(@RequestBody Map<String, Object> request) {
        try {
            Long productId = Long.valueOf(request.get("productId").toString());
            Integer quantity = Integer.valueOf(request.get("quantity").toString());
            LocalDate validUntil = request.get("validUntil") == null
                    ? LocalDate.now().plusDays(7)
                    : LocalDate.parse(request.get("validUntil").toString());
            SocialOffer offer = socialService.publishOffer(productId, quantity, validUntil);
            return ResponseEntity.ok(offerToMap(offer));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage() == null ? "Не удалось опубликовать предложение" : e.getMessage()));
        }
    }

    /**
     * Забрать предложение (только для роли ORG).
     */
    @PostMapping("/offers/{id}/claim")
    public ResponseEntity<?> claimOffer(@PathVariable Long id, org.springframework.security.core.Authentication authentication) {

        try {
            AppUser organization = users.findByEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Организация не найдена"));
            Long orgId = organization.getId();
            SocialOffer offer = socialService.claimOffer(id, orgId);
            Map<String, Object> res = new HashMap<>();
            res.put("message", "Предложение забрано");
            res.put("offer", offerToMap(offer));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my-claims")
    public List<Map<String, Object>> myClaims(org.springframework.security.core.Authentication authentication) {
        AppUser organization = users.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Организация не найдена"));
        return socialService.listClaims(organization.getId()).stream().map(this::offerToMap).toList();
    }

    @GetMapping("/my-claims/{id}/act")
    public ResponseEntity<String> claimAct(@PathVariable Long id, org.springframework.security.core.Authentication authentication) {
        AppUser organization = users.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Организация не найдена"));
        SocialOffer offer = socialService.getClaim(id, organization.getId());
        if(offer.getUnitCostSnapshot()==null) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"Для старой передачи не сохранена стоимость. Требуется ручная сверка документа");
        String productName = esc(offer.getProductNameSnapshot()==null?offer.getProduct().getName():offer.getProductNameSnapshot());
        String productSku = esc(offer.getSkuSnapshot()==null?offer.getProduct().getSku():offer.getSkuSnapshot());
        String reason = esc(offer.getProduct().getAriaReason());
        java.math.BigDecimal unitCost = offer.getUnitCostSnapshot()==null?java.math.BigDecimal.ZERO:offer.getUnitCostSnapshot();
        int quantity = offer.getQuantityClaimed() == null ? 0 : offer.getQuantityClaimed();
        String html = """
                <!doctype html><html lang="ru"><meta charset="utf-8"><title>Акт передачи ARIA-%d</title>
                <style>body{font:15px Arial,sans-serif;max-width:1100px;margin:36px auto;padding:0 24px;color:#172b3a}table{border-collapse:collapse;width:100%%;margin-top:24px}th,td{border:1px solid #ccd6dc;padding:10px;text-align:left}th{background:#edf2f4}.sign{margin-top:72px;display:flex;justify-content:space-between}</style>
                <h1>Акт приёма-передачи ARIA-%d</h1><p>Дата: %s</p><p>Передающая сторона: ARIA Shop</p><p>Принимающая организация: %s</p>
                <p>Партия социальной полки. Количество зарезервировано организацией-получателем.</p>
                <table><tr><th>SKU</th><th>Наименование</th><th>Количество</th><th>Учётная стоимость за единицу, ₸</th><th>Основание</th></tr>
                <tr><td>%s</td><td>%s</td><td>%d</td><td>%s</td><td>%s</td></tr></table>
                <p><strong>Итого к передаче:</strong> %d шт. · <strong>Сумма оценки:</strong> %s ₸ · <strong>Оплата получателем:</strong> 0 ₸</p>
                <p>Черновик: проверьте количество, дату и реквизиты сторон перед подписанием.</p><div class="sign"><span>Передал: ____________________</span><span>Принял: ____________________</span></div>
                </html>
                """.formatted(offer.getId(), offer.getId(),
                offer.getClaimedAt() == null ? java.time.LocalDate.now() : offer.getClaimedAt().toLocalDate(),
                esc(organization.getFullName()), productSku, productName, quantity, unitCost.toPlainString(), reason,
                quantity, unitCost.multiply(java.math.BigDecimal.valueOf(quantity)).toPlainString());
        return ResponseEntity.ok().header("Content-Type", "text/html; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=\"aria-social-act-" + id + ".html\"").body(html);
    }

    private String esc(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String publicReason(String reason) {
        if (reason == null || reason.isBlank()) return "Товар отобран для предотвращения списания";
        String value = reason.toLowerCase(java.util.Locale.ROOT);
        if (value.contains("срок годности")) return "Скоро завершится срок годности";
        if (value.contains("не продаётся")) return "Низкая оборачиваемость товара";
        if (value.contains("маржа")) return "Товар требует ценового решения";
        if (value.contains("рынка")) return "Товар требует ценового решения";
        return "Товар отобран для предотвращения списания";
    }

    private Map<String, Object> offerToMap(SocialOffer offer) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", offer.getId());
        map.put("productId", offer.getProduct().getId());
        map.put("productName", offer.getProduct().getName());
        map.put("socialPrice", offer.getSocialPrice());
        map.put("quantityAvailable", offer.getQuantityAvailable());
        map.put("status", offer.getStatus());
        map.put("claimedQuantity", offer.getQuantityClaimed());
        map.put("validUntil", offer.getValidUntil());
        return map;
    }
}
