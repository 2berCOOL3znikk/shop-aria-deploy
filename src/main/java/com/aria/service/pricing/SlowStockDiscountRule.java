package com.aria.service.pricing;
import com.aria.entity.Product;
import java.time.LocalDate;
import org.springframework.stereotype.Component;
@Component
public class SlowStockDiscountRule implements DiscountRule {
    public int suggest(Product p,LocalDate today,com.aria.entity.PricingPolicy policy){return p.getReceivedAt()!=null&&p.getReceivedAt().isBefore(today.minusDays(policy.getSlowStockDays()))&&Integer.valueOf(0).equals(p.getSalesLast30Days())?policy.getSlowStockPercent():0;}
}
