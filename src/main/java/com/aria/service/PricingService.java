package com.aria.service;

import com.aria.entity.Product;
import com.aria.enums.ProductStatus;
import org.springframework.stereotype.Service;
import java.math.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class PricingService {
    private final List<com.aria.service.pricing.DiscountRule> rules;
    private final PolicyService policies;
    public PricingService(List<com.aria.service.pricing.DiscountRule> rules,PolicyService policies){this.rules=List.copyOf(rules);this.policies=policies;}
    public BigDecimal price(Product p,int percent) {
        if(percent<0 || percent>90) throw new IllegalArgumentException("Скидка должна быть от 0 до 90%");
        if(p.getUnitPrice()==null || p.getUnitPrice().signum()<=0) throw new IllegalArgumentException("Сначала укажите цену продажи");
        return p.getUnitPrice().multiply(BigDecimal.valueOf(100-percent)).divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP);
    }
    public int maximum(Product p) {
        int max=com.aria.service.pricing.PricingMath.maximumDiscount(p.getUnitPrice(),p.getUnitCost(),policies.forProduct(p).getMinimumMarginPercent());
        while(max>0 && (price(p,max).signum()<=0 || p.getMinimumPrice()!=null && price(p,max).compareTo(p.getMinimumPrice())<0))max--;
        return max;
    }
    public void validate(Product p,int percent) {
        price(p,percent);
        if(price(p,percent).signum()<=0 || (p.getMinimumPrice()!=null && price(p,percent).compareTo(p.getMinimumPrice())<0))throw new IllegalStateException("Цена ниже установленного минимума");
        if(p.getStatus()==ProductStatus.SOCIAL || p.getStatus()==ProductStatus.RESERVED || p.getStatus()==ProductStatus.TRANSFERRED || p.getStatus()==ProductStatus.WRITTEN_OFF) throw new IllegalStateException("Скидка недоступна для текущего статуса товара");
        if(p.getExpiryDate()!=null && p.getExpiryDate().isBefore(LocalDate.now())) throw new IllegalStateException("Просроченный товар продавать запрещено");
        if(p.getQuantityOnHand()==null || p.getQuantityOnHand()<1) throw new IllegalStateException("Нет пригодного остатка для продажи");
        if(percent>0 && (p.getUnitCost()==null || percent>maximum(p))) throw new IllegalStateException("Скидка нарушает ограничение себестоимости или минимальной маржи. Допустимый максимум: "+maximum(p)+"%");
    }
    public Map<String,Object> preview(Product p,int percent) {
        BigDecimal price=price(p,percent),cost=p.getUnitCost();
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("percent",percent);result.put("price",price);result.put("unitCost",cost);result.put("maximumPercent",maximum(p));
        result.put("minimumMarginPercent",policies.forProduct(p).getMinimumMarginPercent());
        result.put("profitPerUnit",cost==null?null:price.subtract(cost));
        result.put("marginPercent",cost==null||price.signum()<=0?null:price.subtract(cost).multiply(BigDecimal.valueOf(100)).divide(price,2,RoundingMode.HALF_UP));
        result.put("revenueIfAllSold",price.multiply(BigDecimal.valueOf(p.getQuantityOnHand()==null?0:p.getQuantityOnHand())));
        try {validate(p,percent);result.put("allowed",true);result.put("warning","");}catch(IllegalArgumentException|IllegalStateException e){result.put("allowed",false);result.put("warning",e.getMessage());}
        return result;
    }
    public Map<String,Object> recommendation(Product p) {
        return Map.of("suggestedPercent",0,"reason","Это ручной калькулятор. Рекомендации по продажам и риску партий доступны в разделе ARIA Smart Pricing; фиксированные скидки по числу дней не назначаются.","scenarios",List.of(preview(p,0),preview(p,10),preview(p,20),preview(p,30),preview(p,50)));
    }
}
