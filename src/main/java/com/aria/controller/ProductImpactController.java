package com.aria.controller;

import com.aria.entity.Product;
import com.aria.entity.SocialOffer;
import com.aria.enums.ProductStatus;
import com.aria.repository.ProductRepository;
import com.aria.repository.SocialOfferRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/products")
public class ProductImpactController {
    private final ProductRepository products;
    private final SocialOfferRepository offers;

    public ProductImpactController(ProductRepository products, SocialOfferRepository offers) {
        this.products = products;
        this.offers = offers;
    }

    @GetMapping("/impact")
    public Map<String, Long> getImpact() {
        List<Product> all = products.findAll();
        List<SocialOffer> allOffers = offers.findAll();
        LocalDate today = LocalDate.now();
        long saved = all.stream().filter(p -> p.getStatus() == ProductStatus.TRANSFERRED).count();
        long socialProducts = all.stream().filter(p -> p.getStatus() == ProductStatus.SOCIAL).count();
        long openOfferUnits = allOffers.stream()
                .filter(o -> "OPEN".equals(o.getStatus()))
                .filter(o -> o.getValidUntil() == null || !o.getValidUntil().isBefore(today))
                .mapToLong(o -> o.getQuantityAvailable() == null ? 0 : o.getQuantityAvailable()).sum();
        long social = Math.max(socialProducts, openOfferUnits);
        long processing = all.stream().filter(p -> p.getStatus() == ProductStatus.CANDIDATE || p.getStatus() == ProductStatus.DISCOUNTED).count();
        return Map.of("savedCount", saved, "socialCount", social, "processingCount", processing);
    }

    @GetMapping("/impact/positions")
    public Map<String, Long> getPositionImpact() {
        List<Product> all = products.findAll();
        LocalDate today = LocalDate.now();
        Set<Long> saved = new HashSet<>();
        Set<Long> social = new HashSet<>();
        Set<Long> processing = new HashSet<>();

        for (Product product : all) {
            Long id = product.getId();
            if (id == null) continue;
            if (product.getStatus() == ProductStatus.TRANSFERRED) saved.add(id);
            if (product.getStatus() == ProductStatus.SOCIAL) social.add(id);
            if (product.getStatus() == ProductStatus.CANDIDATE || product.getStatus() == ProductStatus.DISCOUNTED) processing.add(id);
        }
        for (SocialOffer offer : offers.findAll()) {
            if("TRANSFERRED".equals(offer.getStatus())) saved.add(offer.getProduct().getId());
            if (!"OPEN".equals(offer.getStatus()) || (offer.getValidUntil() != null && offer.getValidUntil().isBefore(today))) continue;
            if (offer.getProduct() != null && offer.getProduct().getId() != null) social.add(offer.getProduct().getId());
        }
        return Map.of("savedCount", (long) saved.size(), "socialCount", (long) social.size(), "processingCount", (long) processing.size());
    }
}
