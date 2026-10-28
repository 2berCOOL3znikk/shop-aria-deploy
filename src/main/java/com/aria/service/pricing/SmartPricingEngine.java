package com.aria.service.pricing;

import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Deterministic scenario model. Price response is an explicit sensitivity assumption, not trained ML. */
public final class SmartPricingEngine {
    private SmartPricingEngine(){}
    public record Lot(Long id,int quantity,BigDecimal cost,LocalDate expiry){}
    public record Sale(LocalDateTime shippedAt,int quantity,BigDecimal price){}
    public record PriceExposure(BigDecimal price,double days,long units){}
    public record Facts(Long productId,String sku,String name,String category,LocalDate asOf,
        BigDecimal basePrice,BigDecimal currentPrice,int currentPercent,BigDecimal minimumPrice,int maximumPercent,
        int quantity,int observedDays,List<Lot> lots,List<Sale> sales,List<PriceExposure> exposures,List<String> limitations,Map<String,Object> competitorContext){
        public Facts(Long productId,String sku,String name,String category,LocalDate asOf,BigDecimal basePrice,BigDecimal currentPrice,int currentPercent,BigDecimal minimumPrice,int maximumPercent,int quantity,int observedDays,List<Lot> lots,List<Sale> sales,List<PriceExposure> exposures,List<String> limitations){this(productId,sku,name,category,asOf,basePrice,currentPrice,currentPercent,minimumPrice,maximumPercent,quantity,observedDays,lots,sales,exposures,limitations,Map.of());}
    }
    public record SalesStats(long units7,long units14,long units30,int observedDays,double unitsPerDay,
        Double approximateRateLow,Double approximateRateHigh){}
    public record Scenario(int percent,BigDecimal price,double estimatedUnits,double estimatedUnitsLow,
        double estimatedUnitsHigh,BigDecimal estimatedRevenue,BigDecimal estimatedContribution,
        double estimatedExpiredUnits,double estimatedRemainingShare,String method){}
    public record ResponseModel(String method,Double elasticity,double lower,double upper,int windows,String note){}
    public record Report(Facts facts,SalesStats sales,ResponseModel responseModel,List<Scenario> scenarios,int suggestedPercent,
        BigDecimal suggestedPrice,String action,String confidence,String reason,int reviewAfterHours,
        List<String> limitations){}

    public static Report evaluate(Facts f){
        if(f.basePrice()==null || f.basePrice().signum()<=0 || f.currentPrice()==null || f.currentPrice().signum()<=0)
            throw new IllegalArgumentException("Требуется положительная цена");
        var until=f.asOf().plusDays(1).atStartOfDay();
        long s7=units(f.sales(),until.minusDays(7),until),s14=units(f.sales(),until.minusDays(14),until),s30=units(f.sales(),until.minusDays(30),until);
        int days=Math.max(1,Math.min(30,f.observedDays()));double rate=(double)s30/days;
        // Approximate Poisson interval describes observed sales, not demand or causal discount lift.
        Double low=s30>=10?Math.max(0,(s30-1.96*Math.sqrt(s30))/days):null;
        Double high=s30>=10?(s30+1.96*Math.sqrt(s30))/days:null;
        var stats=new SalesStats(s7,s14,s30,days,round(rate),low==null?null:round(low),high==null?null:round(high));
        var limitations=new ArrayList<>(f.limitations());
        limitations.add("Учтены только подтверждённые отгрузки с известной датой; отменённые и новые заказы не считаются продажами.");
        limitations.add("Периоды отсутствия товара и начала наблюдения не восстановлены. Скорость продаж не равна полному спросу.");
        var model=responseModel(f.exposures());limitations.add(model.note());
        double trend=s30==0?0:(s7/7d)/(s30/30d);
        String confidence=days>=28&&s30>=100&&s7>=20&&model.windows()>=6&&"OBSERVATIONAL_LOG_LINEAR".equals(model.method())&&model.upper()-model.lower()<=1&&trend>=0.5&&trend<=2?"MEDIUM":"LOW";
        limitations.add("MEDIUM".equals(confidence)?"Умеренная уверенность означает достаточность описательных данных по заданным критериям, не причинный эффект, обученную ML-модель или гарантию прогноза.":"Недостаточно устойчивых периодов для автоматического применения выбора AI.");
        limitations.add("AVAILABLE".equals(f.competitorContext().get("status"))?"Учтены импортированные сопоставимые цены конкурентов. Они не доказывают реакцию спроса; достоверной сезонной истории нет.":"Нет проверенных цен конкурентов и достоверной сезонной истории: они не используются.");
        limitations.add("Одна цена на весь SKU. Учтены сроки и себестоимость каждой пригодной партии; горизонт оценки — 30 дней.");
        if(s30<10 || days<14)limitations.add("Мало истории: низкая уверенность. Нулевые продажи не доказывают отсутствие спроса.");
        var percents=new TreeSet<Integer>();percents.add(0);
        for(int i=5;i<=f.maximumPercent();i+=5)percents.add(i);
        percents.add(f.maximumPercent());
        if(f.currentPercent()<=f.maximumPercent())percents.add(f.currentPercent());
        var scenarios=percents.stream().filter(p->p>=0&&p<=90)
            .filter(p->f.basePrice().multiply(BigDecimal.valueOf(100-p)).divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP).compareTo(f.minimumPrice())>=0)
            .map(p->scenario(f,p,rate,model)).toList();
        if(scenarios.isEmpty())return new Report(f,stats,model,List.of(),f.currentPercent(),f.currentPrice(),"REVIEW","LOW","Нет допустимой цены при текущих ограничениях. Исправьте базовую цену, себестоимость или минимум.",24,List.copyOf(limitations));
        Scenario current=scenarios.stream().filter(s->s.percent()==f.currentPercent()).findFirst().orElse(null);
        Scenario best=scenarios.stream().max(Comparator.comparing(Scenario::estimatedContribution)
            .thenComparing(s->s.percent()==f.currentPercent()?1:0).thenComparing(s->-s.percent())).orElseThrow();
        String action;String reason;
        if(s30==0){best=current!=null?current:scenarios.getFirst();action="KEEP";reason="Подтверждённых продаж за 30 дней нет. Нельзя обосновать эффект снижения цены; проверьте доступность товара и спрос. Можно вручную согласовать ограниченный эксперимент.";}
        else if(current!=null && best.estimatedContribution().subtract(current.estimatedContribution()).compareTo(new BigDecimal("0.01"))<=0){best=current;action="KEEP";reason="При заданных допущениях смена цены не улучшает ожидаемый вклад в прибыль после риска списания.";}
        else {action=best.percent()>f.currentPercent()?"DISCOUNT":best.percent()<f.currentPercent()?"RESTORE":"KEEP";
            reason="Выбран допустимый сценарий с наибольшим оценочным вкладом: выручка минус себестоимость проданных и ожидаемо просроченных единиц. Эффект зависит от непроверенного допущения реакции на цену.";}
        if(f.quantity()==0){action="BLOCKED";reason="Нет пригодного доступного остатка. Продажа и применение рекомендации запрещены.";}
        else if(best.estimatedExpiredUnits()>0 && best.estimatedRemainingShare()>0.5){reason+=" Остаётся риск списания: отдельно проверьте пригодность и возможность социальной передачи. Ничего не резервируется автоматически.";}
        int hours=f.lots().stream().anyMatch(l->l.expiry()!=null&&!l.expiry().isAfter(f.asOf().plusDays(1)))?6:24;
        return new Report(f,stats,model,scenarios,best.percent(),best.price(),action,confidence,reason,hours,List.copyOf(limitations));
    }
    private static long units(List<Sale> sales,LocalDateTime from,LocalDateTime until){return sales.stream().filter(s->!s.shippedAt().isBefore(from)&&s.shippedAt().isBefore(until)).mapToLong(Sale::quantity).sum();}
    private record Outcome(double sold,double expired,double contribution){}
    private static Scenario scenario(Facts f,int percent,double rate,ResponseModel model){
        var price=f.basePrice().multiply(BigDecimal.valueOf(100-percent)).divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP);
        double ratio=f.currentPrice().doubleValue()/price.doubleValue();
        double lower=Math.min(Math.pow(ratio,model.lower()),Math.pow(ratio,model.upper()));
        double upper=Math.max(Math.pow(ratio,model.lower()),Math.pow(ratio,model.upper()));
        var pessimistic=outcome(f,price,rate*lower);var optimistic=outcome(f,price,rate*upper);
        // The conservative price response is used for ranking; no claimed fitted elasticity.
        var selected=pessimistic;
        return new Scenario(percent,price,round(selected.sold()),round(pessimistic.sold()),round(optimistic.sold()),
            amount(price.doubleValue()*selected.sold()),amount(selected.contribution()),round(selected.expired()),
            f.quantity()==0?0:round(Math.max(0,f.quantity()-selected.sold())/f.quantity()),model.method());
    }
    private static Outcome outcome(Facts f,BigDecimal price,double rate){
        var lots=f.lots().stream().sorted(Comparator.comparing(Lot::expiry,Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(Lot::id,Comparator.nullsLast(Comparator.naturalOrder()))).toList();
        double usedCapacity=0,sold=0,expired=0,contribution=0;
        for(var lot:lots){
            if(lot.expiry()!=null && lot.expiry().isBefore(f.asOf()))continue;
            long horizon=lot.expiry()==null?30:Math.max(0,Math.min(30,ChronoUnit.DAYS.between(f.asOf(),lot.expiry())+1));
            double units=Math.min(lot.quantity(),Math.max(0,rate*horizon-usedCapacity));usedCapacity+=units;sold+=units;
            double waste=lot.expiry()!=null&&lot.expiry().isBefore(f.asOf().plusDays(30))?lot.quantity()-units:0;
            expired+=waste;contribution+=units*(price.doubleValue()-lot.cost().doubleValue())-waste*lot.cost().doubleValue();
        }
        return new Outcome(sold,expired,contribution);
    }
    private static BigDecimal amount(double value){return BigDecimal.valueOf(value).setScale(2,RoundingMode.HALF_UP);}
    private static double round(double value){return Math.round(value*1000d)/1000d;}
    /** Log-linear observational fit; no causal or trained-ML claim. Unstable fits fall back. */
    public static ResponseModel responseModel(List<PriceExposure> exposures){
        var rows=exposures.stream().filter(e->e.days()>=2&&e.units()>=5&&e.price().signum()>0).toList();
        var fallback=new ResponseModel("HEURISTIC_SENSITIVITY",null,0.25,1.0,rows.size(),"Реакция на цену не установлена: чувствительность 0,25–1,0 — допущение сценария, не обученная ML-модель и не доверительный интервал прогноза.");
        if(rows.size()<4||rows.stream().map(e->e.price().stripTrailingZeros()).distinct().count()<3)return fallback;
        double mx=rows.stream().mapToDouble(e->Math.log(e.price().doubleValue())).average().orElseThrow(),my=rows.stream().mapToDouble(e->Math.log(e.units()/e.days())).average().orElseThrow();
        double xx=0,xy=0;
        for(var row:rows){double x=Math.log(row.price().doubleValue())-mx,y=Math.log(row.units()/row.days())-my;xx+=x*x;xy+=x*y;}
        if(xx<0.01)return fallback;double slope=xy/xx,rss=0;
        for(var row:rows){double residual=Math.log(row.units()/row.days())-my-slope*(Math.log(row.price().doubleValue())-mx);rss+=residual*residual;}
        double uncertainty=3.2*Math.sqrt(rss/(rows.size()-2)/xx),elasticity=-slope;
        if(!Double.isFinite(elasticity)||elasticity<0.1||elasticity>5||uncertainty>elasticity)return fallback;
        return new ResponseModel("OBSERVATIONAL_LOG_LINEAR",round(elasticity),Math.max(0.1,elasticity-uncertainty),Math.min(5,elasticity+uncertainty),rows.size(),"Описательная log-linear регрессия частоты отгрузок на цену по "+rows.size()+" периодам. Не причинный эффект: наличие, сезонность, реклама и задержки отгрузки не контролировались. Чувствительность ограничена 0,1–5; уверенность оценивается отдельно по достаточности и устойчивости данных.");
    }
}
