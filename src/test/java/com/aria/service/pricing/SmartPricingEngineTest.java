package com.aria.service.pricing;
import com.aria.service.pricing.SmartPricingEngine.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class SmartPricingEngineTest {
    private final LocalDate today=LocalDate.of(2026,10,28);
    private Facts facts(List<Lot> lots,List<Sale> sales,List<PriceExposure> windows){return new Facts(1L,"SKU","Товар","Продукты",today,new BigDecimal("1000"),new BigDecimal("1000"),0,new BigDecimal("800"),20,lots.stream().mapToInt(Lot::quantity).sum(),30,lots,sales,windows,List.of());}
    private Lot lot(long id,int qty,int days){return new Lot(id,qty,new BigDecimal("800"),today.plusDays(days));}
    private Sale sale(int days,int qty){return new Sale(today.minusDays(days).atTime(12,0),qty,new BigDecimal("1000"));}
    @Test void noSalesDoesNotInventDemandOrClaimMachineLearning(){
        var r=SmartPricingEngine.evaluate(facts(List.of(lot(1,100,2)),List.of(),List.of()));
        assertThat(r.sales().units30()).isZero();assertThat(r.action()).isEqualTo("KEEP");assertThat(r.suggestedPercent()).isZero();assertThat(r.confidence()).isEqualTo("LOW");
        assertThat(r.scenarios()).allMatch(s->s.estimatedRevenue().signum()==0&&s.estimatedUnits()==0);assertThat(r.responseModel().elasticity()).isNull();
    }
    @Test void calendarWindowsExcludeOldAndFutureShipments(){
        var r=SmartPricingEngine.evaluate(facts(List.of(lot(1,100,40)),List.of(sale(0,1),sale(6,2),sale(7,4),sale(13,8),sale(14,16),sale(29,32),sale(30,64),sale(-1,128)),List.of()));
        assertThat(r.sales().units7()).isEqualTo(3);assertThat(r.sales().units14()).isEqualTo(15);assertThat(r.sales().units30()).isEqualTo(63);
    }
    @Test void fefoKeepsSeparateDeadlinesAndCostFloor(){
        var r=SmartPricingEngine.evaluate(facts(List.of(lot(1,10,0),lot(2,20,29)),List.of(sale(2,30)),List.of()));
        var current=r.scenarios().getFirst();assertThat(current.estimatedUnits()).isEqualTo(21);assertThat(current.estimatedExpiredUnits()).isEqualTo(9);
        assertThat(r.scenarios()).allMatch(s->s.price().compareTo(new BigDecimal("800"))>=0);assertThat(r.reviewAfterHours()).isEqualTo(6);
    }
    @Test void insufficientOrUnstablePriceHistoryUsesExplicitHeuristic(){
        assertThat(SmartPricingEngine.responseModel(List.of(new PriceExposure(new BigDecimal("1000"),10,30))).method()).isEqualTo("HEURISTIC_SENSITIVITY");
        var increasing=List.of(new PriceExposure(new BigDecimal("800"),4,8),new PriceExposure(new BigDecimal("900"),4,9),new PriceExposure(new BigDecimal("1000"),4,10),new PriceExposure(new BigDecimal("1100"),4,11));
        assertThat(SmartPricingEngine.responseModel(increasing).elasticity()).isNull();
    }
    @Test void consistentObservedPriceResponseCanChooseDiscountWithoutDayThresholds(){
        var windows=List.of(new PriceExposure(new BigDecimal("800"),4,625),new PriceExposure(new BigDecimal("1000"),4,400),new PriceExposure(new BigDecimal("1250"),4,256),new PriceExposure(new BigDecimal("1600"),4,156));
        var model=SmartPricingEngine.responseModel(windows);assertThat(model.method()).isEqualTo("OBSERVATIONAL_LOG_LINEAR");assertThat(model.elasticity()).isBetween(1.9,2.1);
        var r=SmartPricingEngine.evaluate(facts(List.of(lot(1,100,2)),List.of(sale(1,30)),windows));
        assertThat(r.suggestedPercent()).isGreaterThan(0);assertThat(r.suggestedPercent()).isLessThanOrEqualTo(20);assertThat(r.confidence()).isEqualTo("LOW");
    }
    @Test void nonPerishableResidualIsNotCalledExpiryLoss(){
        var f=facts(List.of(new Lot(1L,100,new BigDecimal("800"),null)),List.of(sale(1,30)),List.of());
        assertThat(SmartPricingEngine.evaluate(f).scenarios()).allMatch(s->s.estimatedExpiredUnits()==0);
    }
}
