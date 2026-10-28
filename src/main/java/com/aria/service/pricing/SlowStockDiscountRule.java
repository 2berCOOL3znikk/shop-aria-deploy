package com.aria.service.pricing;
import com.aria.entity.Product;
import java.time.LocalDate;
import org.springframework.stereotype.Component;
@Component
public class SlowStockDiscountRule implements DiscountRule {
    public int suggest(Product p,LocalDate today){return p.getReceivedAt()!=null&&p.getReceivedAt().isBefore(today.minusDays(180))&&Integer.valueOf(0).equals(p.getSalesLast30Days())?10:0;}
}
