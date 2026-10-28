package com.aria.service.pricing;
import com.aria.entity.Product;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;
@Component
public class ExpiryDiscountRule implements DiscountRule {
    public int suggest(Product p,LocalDate today,com.aria.entity.PricingPolicy policy){
        if(p.getExpiryDate()==null)return 0;
        long days=ChronoUnit.DAYS.between(today,p.getExpiryDate());
        return days<0?0:days<=policy.getClearanceDays()?policy.getClearancePercent():days<=policy.getCriticalDays()?policy.getCriticalPercent():days<=policy.getWarningDays()?policy.getWarningPercent():0;
    }
}
