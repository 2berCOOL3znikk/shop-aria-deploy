package com.aria.service;

import com.aria.entity.Product;
import com.aria.entity.SocialOffer;
import com.aria.enums.ProductStatus;
import com.aria.repository.ProductRepository;
import com.aria.repository.SocialOfferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class SocialService {

    private final SocialOfferRepository socialOfferRepo;
    private final ProductRepository productRepo;

    public SocialService(SocialOfferRepository socialOfferRepo, ProductRepository productRepo) {
        this.socialOfferRepo = socialOfferRepo;
        this.productRepo = productRepo;
    }

    /**
     * Публикует социальное предложение на основе товара.
     * Товар должен быть DISCOUNTED или SOCIAL.
     * socialPrice вычисляется как скидка от unitPrice (например, 70% от цены).
     */
    @Transactional
    public SocialOffer publishOffer(Long productId, Integer quantity, LocalDate validUntil) {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

        if (product.getStatus() != ProductStatus.CANDIDATE && product.getStatus() != ProductStatus.DISCOUNTED && product.getStatus() != ProductStatus.SOCIAL) {
            throw new IllegalStateException("На соцполку можно отправить только товар-кандидат или товар со скидкой");
        }

        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Укажите количество больше нуля");
        }
        if (product.getExpiryDate() != null && product.getExpiryDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException("Просроченные товары нельзя публиковать на соцполке");
        }
        if (validUntil == null) validUntil = LocalDate.now().plusDays(7);
        if (validUntil.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Срок предложения не может быть в прошлом");
        }
        if (product.getExpiryDate() != null && validUntil.isAfter(product.getExpiryDate())) {
            validUntil = product.getExpiryDate();
        }
        int alreadyOffered = socialOfferRepo.findByProductId(productId).stream()
                .filter(offer -> "OPEN".equals(offer.getStatus()))
                .mapToInt(SocialOffer::getQuantityAvailable).sum();
        if (product.getQuantityOnHand() == null || alreadyOffered + quantity > product.getQuantityOnHand()) {
            throw new IllegalArgumentException("Недостаточно товара на складе");
        }

        // Предложение на соцполке — благотворительная передача, а не платная распродажа.
        BigDecimal socialPrice = BigDecimal.ZERO;
        // Пока лот опубликован, товар исключён из обычной витрины — иначе его могут купить и зарезервировать фонд.
        product.setStatus(ProductStatus.SOCIAL);

        SocialOffer offer = new SocialOffer(product, socialPrice, quantity, validUntil);
        return socialOfferRepo.save(offer);
    }

    /**
     * Забирает предложение организацией (только для роли ORG).
     * Уменьшает quantityAvailable и устанавливает CLAIMED.
     */
    @Transactional
    public SocialOffer claimOffer(Long offerId, Long orgId) {
        SocialOffer offer = socialOfferRepo.findById(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Предложение не найдено"));

        if (!"OPEN".equals(offer.getStatus())) {
            throw new IllegalStateException("Предложение уже забрано или закрыто");
        }

        if (offer.getQuantityAvailable() <= 0) {
            throw new IllegalStateException("Товар закончился");
        }

        if (offer.getValidUntil() != null && offer.getValidUntil().isBefore(LocalDate.now())) {
            throw new IllegalStateException("Срок предложения истёк");
        }

        if (offer.getProduct().getExpiryDate() != null && offer.getProduct().getExpiryDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException("Срок годности товара истёк");
        }
        int claimedQuantity = offer.getQuantityAvailable();
        offer.setQuantityAvailable(0);
        offer.setQuantityClaimed(claimedQuantity);
        offer.setOrgId(orgId);
        offer.setStatus("CLAIMED");
        offer.setClaimedAt(java.time.LocalDateTime.now());
        Product product = offer.getProduct();
        product.setQuantityOnHand(Math.max(0, product.getQuantityOnHand() - claimedQuantity));
        if (product.getQuantityOnHand() == 0) product.setStatus(ProductStatus.TRANSFERRED);

        return socialOfferRepo.save(offer);
    }

    /**
     * Возвращает список открытых предложений.
     * НЕ включает orgId в ответ для сохранения приватности.
     */
    @Transactional(readOnly = true)
    public List<SocialOffer> listOpenOffers() {
        return socialOfferRepo.findByStatusOrderByCreatedAtDesc("OPEN").stream()
                .filter(offer -> offer.getValidUntil() == null || !offer.getValidUntil().isBefore(LocalDate.now()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getPublicImpact() {
        List<SocialOffer> offers = socialOfferRepo.findAll();
        LocalDate today = LocalDate.now();
        long openUnits = offers.stream()
                .filter(offer -> "OPEN".equals(offer.getStatus()))
                .filter(offer -> offer.getValidUntil() == null || !offer.getValidUntil().isBefore(today))
                .mapToLong(offer -> offer.getQuantityAvailable() == null ? 0 : offer.getQuantityAvailable())
                .sum();
        long claimedUnits = offers.stream()
                .filter(offer -> "CLAIMED".equals(offer.getStatus()))
                .mapToLong(offer -> offer.getQuantityClaimed() == null ? 0 : offer.getQuantityClaimed())
                .sum();
        return Map.of("openUnits", openUnits, "claimedUnits", claimedUnits);
    }

    @Transactional(readOnly = true)
    public List<SocialOffer> listClaims(Long orgId) {
        return socialOfferRepo.findByOrgIdAndStatusOrderByCreatedAtDesc(orgId, "CLAIMED");
    }

    @Transactional(readOnly = true)
    public SocialOffer getClaim(Long offerId, Long orgId) {
        return socialOfferRepo.findByIdAndOrgIdAndStatus(offerId, orgId, "CLAIMED")
                .orElseThrow(() -> new IllegalArgumentException("Передача не найдена для этой организации"));
    }
}
