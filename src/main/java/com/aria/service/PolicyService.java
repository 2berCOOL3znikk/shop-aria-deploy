package com.aria.service;

import com.aria.dto.ControlRequests.PolicyInput;
import com.aria.entity.PricingPolicy;
import com.aria.entity.Product;
import com.aria.repository.PricingPolicyRepository;
import com.aria.repository.ProductRepository;
import com.aria.service.pricing.PricingMath;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class PolicyService {
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    private final PricingPolicyRepository policies;
    private final ProductRepository products;
    private final AuditService audit;

    public PolicyService(PricingPolicyRepository policies,ProductRepository products,AuditService audit) {
        this.policies=policies;this.products=products;this.audit=audit;
    }
    public static String categoryKey(String value) {
        return value==null||value.isBlank()?"*":value.trim().toLowerCase(Locale.ROOT);
    }
    @Transactional(readOnly=true)
    public PricingPolicy forProduct(Product product) {
        return policies.findByCategory(categoryKey(product.getCategory()))
            .or(()->policies.findByCategory("*")).orElseGet(PricingPolicy::new);
    }
    @Transactional(readOnly=true)
    public List<PricingPolicy> list() {
        var result=new ArrayList<>(policies.findAll());
        if(result.stream().noneMatch(p->"*".equals(p.getCategory())))result.addFirst(new PricingPolicy());
        return result;
    }
    @Transactional
    public PricingPolicy save(PolicyInput input) {
        if(input.clearanceDays()>input.criticalDays() || input.criticalDays()>input.warningDays())
            throw new IllegalArgumentException("Пороги дней: экстренный ≤ критический ≤ предупреждение");
        if(input.warningPercent()>input.criticalPercent() || input.criticalPercent()>input.clearancePercent())
            throw new IllegalArgumentException("Скидка должна возрастать при сокращении срока");
        String category=categoryKey(input.category());
        var overrides=policies.findAll().stream().map(PricingPolicy::getCategory).filter(c->!"*".equals(c)).collect(java.util.stream.Collectors.toSet());
        var affected=products.findAll().stream()
            .filter(p->category.equals(categoryKey(p.getCategory())) || ("*".equals(category)&&!overrides.contains(categoryKey(p.getCategory()))))
            .sorted(Comparator.comparing(Product::getId)).toList();
        for(Product p:affected){products.lockById(p.getId());entityManager.refresh(p);}
        var conflicts=affected.stream().filter(p->!p.getArchived()&&p.getStatus()==com.aria.enums.ProductStatus.DISCOUNTED&&p.getQuantityOnHand()>0&&p.getDiscountPercent()!=null&&p.getDiscountPercent()>0)
            .filter(p->p.getDiscountPercent()>PricingMath.maximumDiscount(p.getUnitPrice(),p.getUnitCost(),input.minimumMarginPercent()))
            .limit(3).map(Product::getName).toList();
        if(!conflicts.isEmpty())throw new IllegalStateException("Сначала уменьшите действующие скидки: "+String.join(", ",conflicts));
        PricingPolicy policy=policies.findByCategory(category).orElseGet(PricingPolicy::new);
        policy.setCategory(category);policy.setWarningDays(input.warningDays());policy.setCriticalDays(input.criticalDays());policy.setClearanceDays(input.clearanceDays());
        policy.setWarningPercent(input.warningPercent());policy.setCriticalPercent(input.criticalPercent());policy.setClearancePercent(input.clearancePercent());
        policy.setSlowStockDays(input.slowStockDays());policy.setSlowStockPercent(input.slowStockPercent());policy.setMinimumMarginPercent(input.minimumMarginPercent());
        policies.save(policy);
        audit.record("PRICING_POLICY_CHANGED","category:"+category,"Пороги "+policy.getWarningDays()+"/"+policy.getCriticalDays()+"/"+policy.getClearanceDays()+" дн.; скидки "+policy.getWarningPercent()+"/"+policy.getCriticalPercent()+"/"+policy.getClearancePercent()+"%; минимальная маржа "+policy.getMinimumMarginPercent()+"%");
        return policy;
    }
    @Transactional
    public void resetCategory(String category) {
        String key=categoryKey(category);
        if("*".equals(key))throw new IllegalArgumentException("Общее правило нельзя удалить. Измените его параметры");
        PricingPolicy current=policies.findByCategory(key).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Правило категории не найдено"));
        PricingPolicy fallback=policies.findByCategory("*").orElseGet(PricingPolicy::new);
        var affected=products.findAll().stream().filter(p->key.equals(categoryKey(p.getCategory()))).sorted(Comparator.comparing(Product::getId)).toList();
        for(Product p:affected){products.lockById(p.getId());entityManager.refresh(p);
            if(!p.getArchived()&&p.getStatus()==com.aria.enums.ProductStatus.DISCOUNTED&&p.getQuantityOnHand()>0&&p.getDiscountPercent()!=null&&p.getDiscountPercent()>PricingMath.maximumDiscount(p.getUnitPrice(),p.getUnitCost(),fallback.getMinimumMarginPercent()))
                throw new IllegalStateException("Сначала уменьшите действующую скидку товара «"+p.getName()+"»");
        }
        policies.delete(current);audit.record("PRICING_POLICY_RESET","category:"+key,"Категория наследует общее правило магазина");
    }
}
