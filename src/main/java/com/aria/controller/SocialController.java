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
    @org.springframework.beans.factory.annotation.Value("${aria.organization.key:demo}") private String organizationKey;

    private final SocialService socialService;
    private final AppUserRepository users;
    private final com.aria.service.SocialActService acts;

    public SocialController(SocialService socialService, AppUserRepository users, com.aria.service.SocialActService acts) {
        this.socialService = socialService;
        this.users = users;
        this.acts = acts;
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
            String image=offer.getProduct().getImageUrl();
            map.put("imageUrl",!"demo".equals(organizationKey)&&("/api/products/"+offer.getProduct().getId()+"/photo").equals(image)?"/api/social/offers/"+offer.getId()+"/photo":image);
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
        return acts.document(offer,organization.getFullName());
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
