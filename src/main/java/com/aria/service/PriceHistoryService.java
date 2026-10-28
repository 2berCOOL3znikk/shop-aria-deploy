package com.aria.service;
import com.aria.entity.*;
import com.aria.repository.PriceChangeRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class PriceHistoryService {
    private final PriceChangeRepository changes;
    public PriceHistoryService(PriceChangeRepository changes){this.changes=changes;}
    public PriceChange record(Product p,BigDecimal oldBase,int oldPercent,int newPercent,String reason){
        if(oldBase.compareTo(p.getUnitPrice())==0 && oldPercent==newPercent)return null;
        var e=new PriceChange();e.productId=p.getId();e.changedAt=LocalDateTime.now();
        var auth=SecurityContextHolder.getContext().getAuthentication();e.actor=auth==null?"system":auth.getName();
        e.reason=reason;e.oldBasePrice=oldBase;e.newBasePrice=p.getUnitPrice();e.oldPercent=oldPercent;e.newPercent=newPercent;
        e.costAtChange=p.getUnitCost()==null?BigDecimal.ZERO:p.getUnitCost();e.availableAtChange=p.getQuantityOnHand();
        return changes.saveAndFlush(e);
    }
}
