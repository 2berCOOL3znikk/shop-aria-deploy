package com.aria.service.pricing;
import com.aria.entity.Product;
import java.time.LocalDate;
/** Strategy: each independent business rule proposes a percentage; cost limits are applied centrally. */
public interface DiscountRule { int suggest(Product product, LocalDate today,com.aria.entity.PricingPolicy policy); }
