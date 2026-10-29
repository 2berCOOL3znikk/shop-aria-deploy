package com.aria.service.pricing;

import java.time.temporal.ChronoUnit;

/** Explicit, conservative planning heuristic. Reporting windows are not product lifetime rules. */
public record PlanningWindow(int observationDays,int forecastDays,Double clearanceDays,Integer nearestExpiryDays,
                             int reviewAfterHours,String basis) {
    public static PlanningWindow from(SmartPricingEngine.Facts f,double rate,long recordedUnits) {
        int observed=Math.max(1,f.observedDays());
        Integer expiry=f.lots().stream().filter(l->l.expiry()!=null&&!l.expiry().isBefore(f.asOf()))
            .mapToInt(l->(int)Math.min(36500,ChronoUnit.DAYS.between(f.asOf(),l.expiry())+1)).min().stream().boxed().findFirst().orElse(null);
        Double clearance=rate>0?Math.ceil(f.quantity()/rate*100d)/100d:null;
        // More sampling uncertainty shortens the extrapolation; this is not a calibrated forecast interval.
        int supported=Math.max(1,(int)Math.floor(observed/(1+1.96/Math.sqrt(Math.max(1,recordedUnits)))));
        int horizon=Math.min(supported,clearance==null?supported:Math.max(1,(int)Math.ceil(Math.min(36500,clearance))));
        if(expiry!=null)horizon=Math.min(horizon,expiry);
        double boundary=expiry==null?Double.POSITIVE_INFINITY:expiry*24d;
        if(clearance!=null&&clearance>0)boundary=Math.min(boundary,clearance*24);
        int review=(int)Math.max(1,Math.min(72,Math.floor(Math.min(boundary/4,horizon*24d/3))));
        return new PlanningWindow(observed,horizon,clearance,expiry,review,
            "Горизонт ограничен фактической историей, её выборочной неопределённостью, оценкой реализации запаса и ближайшим expiryDate. Это осторожная эвристика, не правило категории и не гарантия спроса. Дата пересмотра задаётся отдельно; срок годности не назначается по названию товара.");
    }
}
