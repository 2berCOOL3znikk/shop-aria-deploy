package com.aria.service.pricing;
import com.aria.entity.Product;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;
@Component
public class ExpiryDiscountRule implements DiscountRule {
    public int suggest(Product p,LocalDate today){
        if(p.getExpiryDate()==null)return 0;
        long days=ChronoUnit.DAYS.between(today,p.getExpiryDate());
        return days<0?0:days<=1?50:days<=7?30:days<=30?15:0;
    }
}
