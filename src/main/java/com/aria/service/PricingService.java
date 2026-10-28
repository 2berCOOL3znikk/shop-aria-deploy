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
    public PricingService(List<com.aria.service.pricing.DiscountRule> rules){this.rules=List.copyOf(rules);}
    public BigDecimal price(Product p,int percent) {
        if(percent<0 || percent>90) throw new IllegalArgumentException("Скидка должна быть от 0 до 90%");
        if(p.getUnitPrice()==null || p.getUnitPrice().signum()<=0) throw new IllegalArgumentException("Сначала укажите цену продажи");
        return p.getUnitPrice().multiply(BigDecimal.valueOf(100-percent)).divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP);
    }
    public int maximum(Product p) {
        if(p.getUnitPrice()==null || p.getUnitPrice().signum()<=0 || p.getUnitCost()==null) return 0;
        return Math.max(0,Math.min(90,BigDecimal.ONE.subtract(p.getUnitCost().divide(p.getUnitPrice(),10,RoundingMode.CEILING)).multiply(BigDecimal.valueOf(100)).setScale(0,RoundingMode.FLOOR).intValue()));
    }
    public void validate(Product p,int percent) {
        price(p,percent);
        if(p.getStatus()==ProductStatus.SOCIAL || p.getStatus()==ProductStatus.RESERVED || p.getStatus()==ProductStatus.TRANSFERRED || p.getStatus()==ProductStatus.WRITTEN_OFF) throw new IllegalStateException("Скидка недоступна для текущего статуса товара");
        if(p.getExpiryDate()!=null && p.getExpiryDate().isBefore(LocalDate.now())) throw new IllegalStateException("Просроченный товар продавать запрещено");
        if(p.getQuantityOnHand()==null || p.getQuantityOnHand()<1) throw new IllegalStateException("Нет пригодного остатка для продажи");
        if(percent>0 && (p.getUnitCost()==null || price(p,percent).compareTo(p.getUnitCost())<0)) throw new IllegalStateException("Цена после скидки ниже себестоимости. Максимум без убытка: "+maximum(p)+"%");
    }
    public Map<String,Object> preview(Product p,int percent) {
        BigDecimal price=price(p,percent),cost=p.getUnitCost();
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("percent",percent);result.put("price",price);result.put("unitCost",cost);result.put("maximumPercent",maximum(p));
        result.put("profitPerUnit",cost==null?null:price.subtract(cost));
        result.put("marginPercent",cost==null?null:price.subtract(cost).multiply(BigDecimal.valueOf(100)).divide(price,2,RoundingMode.HALF_UP));
        result.put("revenueIfAllSold",price.multiply(BigDecimal.valueOf(p.getQuantityOnHand()==null?0:p.getQuantityOnHand())));
        try {validate(p,percent);result.put("allowed",true);result.put("warning","");}catch(IllegalArgumentException|IllegalStateException e){result.put("allowed",false);result.put("warning",e.getMessage());}
        return result;
    }
    public Map<String,Object> recommendation(Product p) {
        long days=p.getExpiryDate()==null?Long.MAX_VALUE:ChronoUnit.DAYS.between(LocalDate.now(),p.getExpiryDate());
        int target=days<0?0:rules.stream().mapToInt(rule->rule.suggest(p,LocalDate.now())).max().orElse(0);
        int suggested=Math.min(target,maximum(p));
        String reason=days<0?"Срок истёк: продажа запрещена. Проверьте партии и оформите списание.":target==0?"Нет основания для скидки по срокам или оборачиваемости.":"Правило предлагает "+target+"%; ограничение по себестоимости — "+maximum(p)+"%. Итог: "+suggested+"%.";
        return Map.of("suggestedPercent",suggested,"reason",reason,"scenarios",List.of(preview(p,0),preview(p,10),preview(p,20),preview(p,30),preview(p,50)));
    }
}
