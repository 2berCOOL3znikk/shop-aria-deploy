package com.aria.service.pricing;
import com.aria.service.pricing.SmartPricingEngine.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
class PlanningWindowTest {
 private final LocalDate now=LocalDate.of(2026,10,29);
 private Facts facts(String category,LocalDate expiry,int quantity){return new Facts(1L,"SKU","Товар",category,now,new BigDecimal("1000"),new BigDecimal("1000"),0,new BigDecimal("650"),35,quantity,30,List.of(new Lot(1L,quantity,new BigDecimal("600"),expiry)),List.of(new Sale(now.minusDays(1).atTime(12,0),60,new BigDecimal("1000"))),List.of(),List.of());}
 @Test void expiryCoverageForecastAndReviewAreSeparateDataDrivenValues(){
  var bread=SmartPricingEngine.evaluate(facts("Хлеб",now,100)).planning();var canned=SmartPricingEngine.evaluate(facts("Консервы",now.plusDays(300),100)).planning();var electronics=SmartPricingEngine.evaluate(facts("Электроника",null,10)).planning();
  assertThat(bread.forecastDays()).isEqualTo(1);assertThat(canned.nearestExpiryDays()).isEqualTo(301);assertThat(canned.forecastDays()).isLessThan(canned.nearestExpiryDays()).isGreaterThan(bread.forecastDays());assertThat(electronics.nearestExpiryDays()).isNull();assertThat(electronics.forecastDays()).isLessThan(canned.forecastDays());assertThat(canned.clearanceDays()).isEqualTo(50d);assertThat(canned.reviewAfterHours()).isNotEqualTo(canned.forecastDays());
 }
 @Test void changingCategoryCannotInventOrChangeActualExpiry(){assertThat(SmartPricingEngine.evaluate(facts("Молоко",null,30)).planning()).isEqualTo(SmartPricingEngine.evaluate(facts("Электроника",null,30)).planning());}
 @Test void allAllowedIntegerDiscountsAreEvaluatedInsteadOfFivePercentTable(){var report=SmartPricingEngine.evaluate(facts("Продукты",now.plusDays(4),100));assertThat(report.scenarios()).anyMatch(x->x.percent()==1).anyMatch(x->x.percent()==7);assertThat(report.scenarios()).allMatch(x->x.price().compareTo(new BigDecimal("650"))>=0);}
}
