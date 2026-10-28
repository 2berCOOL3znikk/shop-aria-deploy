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
    private final InventoryService inventory;
    private final AuditService audit;
    private final com.aria.repository.OrganizationVerificationRepository verifications;

    public SocialService(SocialOfferRepository socialOfferRepo, ProductRepository productRepo, InventoryService inventory, AuditService audit, com.aria.repository.OrganizationVerificationRepository verifications) {
        this.socialOfferRepo = socialOfferRepo;
        this.productRepo = productRepo;
        this.inventory=inventory;this.audit=audit;this.verifications=verifications;
    }

    /**
     * Публикует социальное предложение на основе товара.
     * Товар должен быть DISCOUNTED или SOCIAL.
     * socialPrice вычисляется как скидка от unitPrice (например, 70% от цены).
     */
    @Transactional
    public SocialOffer publishOffer(Long productId, Integer quantity, LocalDate validUntil) {
        Product product = inventory.lock(productId);
        inventory.ensureLegacyLot(product); inventory.synchronize(product);
        if(product.getArchived()) throw new IllegalStateException("Товар архивирован");

        if (product.getStatus() != ProductStatus.ACTIVE && product.getStatus() != ProductStatus.CANDIDATE && product.getStatus() != ProductStatus.DISCOUNTED) {
            throw new IllegalStateException("Товар уже участвует в передаче или недоступен. Сначала завершите действующее предложение");
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
        offer.snapshotProduct();
        socialOfferRepo.save(offer);audit.record("SOCIAL_PUBLISHED","offer:"+offer.getId(),product.getSku()+" · "+quantity+" ед. · до "+validUntil);return offer;
    }

    /**
     * Забирает предложение организацией (только для роли ORG).
     * Уменьшает quantityAvailable и устанавливает CLAIMED.
     */
    @Transactional
    public SocialOffer claimOffer(Long offerId, Long orgId) {
        SocialOffer offer = socialOfferRepo.lockById(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Предложение не найдено"));
        if(verifications.findByUserId(orgId).filter(v->"VERIFIED".equals(v.getStatus())).isEmpty()) throw new IllegalStateException("Сначала дождитесь подтверждения организации");
        Product locked=inventory.lock(offer.getProduct().getId());

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
        if(inventory.available(locked)<claimedQuantity) throw new IllegalStateException("Недостаточно пригодного остатка");
        offer.setQuantityAvailable(0);
        offer.setQuantityClaimed(claimedQuantity);
        offer.setOrgId(orgId);
        offer.setStatus("CLAIMED");
        offer.setClaimedAt(java.time.LocalDateTime.now());
        Product product = offer.getProduct();
        product.setStatus(ProductStatus.RESERVED);
        audit.record("SOCIAL_RESERVED","offer:"+offerId,"Организация "+orgId+" · "+claimedQuantity+" ед.");

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
                .filter(offer -> "TRANSFERRED".equals(offer.getStatus()))
                .mapToLong(offer -> offer.getQuantityClaimed() == null ? 0 : offer.getQuantityClaimed())
                .sum();
        long reservedUnits=offers.stream().filter(o->"CLAIMED".equals(o.getStatus())).mapToLong(o->o.getQuantityClaimed()==null?0:o.getQuantityClaimed()).sum();
        return Map.of("openUnits", openUnits, "claimedUnits", reservedUnits,"transferredUnits",claimedUnits);
    }

    @Transactional(readOnly = true)
    public List<SocialOffer> listClaims(Long orgId) {
        return socialOfferRepo.findByOrgIdOrderByCreatedAtDesc(orgId).stream().filter(o->"CLAIMED".equals(o.getStatus()) || "TRANSFERRED".equals(o.getStatus())).toList();
    }

    @Transactional(readOnly = true)
    public SocialOffer getClaim(Long offerId, Long orgId) {
        return socialOfferRepo.findById(offerId).filter(o->orgId.equals(o.getOrgId()) && ("CLAIMED".equals(o.getStatus())||"TRANSFERRED".equals(o.getStatus())))
                .orElseThrow(() -> new IllegalArgumentException("Передача не найдена для этой организации"));
    }
    @Transactional
    public SocialOffer finish(Long id,boolean cancel,String reason) {
        SocialOffer offer=socialOfferRepo.lockById(id).orElseThrow(()->new IllegalArgumentException("Предложение не найдено"));
        if(!"CLAIMED".equals(offer.getStatus()) && !(cancel&&"OPEN".equals(offer.getStatus()))) throw new IllegalStateException("Операция недоступна в текущем статусе");
        Product p=inventory.lock(offer.getProduct().getId());
        if(cancel){offer.setStatus("CANCELLED");p.setStatus(ProductStatus.CANDIDATE);}
        else {
            if(offer.getValidUntil()!=null && offer.getValidUntil().isBefore(LocalDate.now())) throw new IllegalStateException("Срок выдачи истёк. Отмените резерв и проверьте партии");
            inventory.consume(p,offer.getQuantityClaimed(),"SOCIAL_TRANSFER_STOCK");
            offer.setStatus("TRANSFERRED"); p.setStatus(p.getQuantityOnHand()==0?ProductStatus.TRANSFERRED:ProductStatus.CANDIDATE);
            offer.setTransferredAt(java.time.LocalDateTime.now());
        }
        audit.record(cancel?"SOCIAL_CANCELLED":"SOCIAL_TRANSFERRED","offer:"+id,reason);return socialOfferRepo.save(offer);
    }
}
