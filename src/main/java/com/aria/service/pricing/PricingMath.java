package com.aria.service.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PricingMath {
    private PricingMath() {}

    /** Margin is profit divided by sale price, not markup on cost. Always rounds conservatively. */
    public static int maximumDiscount(BigDecimal price,BigDecimal cost,int minimumMarginPercent) {
        if(price==null || price.signum()<=0 || cost==null) return 0;
        BigDecimal remaining=BigDecimal.valueOf(100-minimumMarginPercent).movePointLeft(2);
        BigDecimal minimumPrice=cost.divide(remaining,10,RoundingMode.CEILING);
        return Math.max(0,Math.min(90,BigDecimal.ONE.subtract(minimumPrice.divide(price,10,RoundingMode.CEILING))
            .multiply(BigDecimal.valueOf(100)).setScale(0,RoundingMode.FLOOR).intValue()));
    }
}
