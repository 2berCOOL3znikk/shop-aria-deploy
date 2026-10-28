package com.aria.service;
import com.aria.entity.Product;
import com.aria.service.pricing.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
class PricingServiceTest {
    private final PolicyService policies=new PolicyService(org.mockito.Mockito.mock(com.aria.repository.PricingPolicyRepository.class),org.mockito.Mockito.mock(com.aria.repository.ProductRepository.class),org.mockito.Mockito.mock(AuditService.class));
    private final PricingService pricing=new PricingService(List.of(new ExpiryDiscountRule(),new SlowStockDiscountRule()),policies);
    private Product product(String price,String cost){Product p=new Product();p.setUnitPrice(new BigDecimal(price));p.setUnitCost(new BigDecimal(cost));p.setQuantityOnHand(10);p.setReceivedAt(LocalDate.now());return p;}
    @Test void capsDiscountAtCostAndShowsProfit(){Product p=product("1000","800");assertThat(pricing.maximum(p)).isEqualTo(20);assertThat(pricing.preview(p,20)).containsEntry("allowed",true).containsEntry("profitPerUnit",new BigDecimal("0.00"));assertThat(pricing.preview(p,21)).containsEntry("allowed",false);}
    @Test void percentageIsRoundedDownWithoutAllowingLoss(){assertThat(pricing.maximum(product("99.99","80"))).isEqualTo(19);}
    @Test void expiredProductHasNoDiscountAndCannotBeSold(){Product p=product("1000","100");p.setExpiryDate(LocalDate.now().minusDays(1));assertThat(pricing.recommendation(p)).containsEntry("suggestedPercent",0);assertThat(pricing.preview(p,0)).containsEntry("allowed",false);}
    @Test void manualCalculatorDoesNotInventRecommendationsFromDayThresholds(){Product p=product("1000","800");p.setExpiryDate(LocalDate.now().plusDays(7));assertThat(pricing.recommendation(p)).containsEntry("suggestedPercent",0);p.setExpiryDate(LocalDate.now().plusDays(8));assertThat(pricing.recommendation(p)).containsEntry("suggestedPercent",0);}
    @Test void explicitMinimumPriceCapsEveryDiscount(){Product p=product("1000","800");p.setMinimumPrice(new BigDecimal("950"));assertThat(pricing.maximum(p)).isEqualTo(5);assertThat(pricing.preview(p,6)).containsEntry("allowed",false);}
    @Test void subKopeckRoundedPriceCannotBecomeZeroOrDivideByZero(){Product p=product("0.01","0.00");assertThat(pricing.maximum(p)).isEqualTo(50);assertThat(pricing.preview(p,90)).containsEntry("allowed",false).containsEntry("marginPercent",null);}
    @Test void badPercentAndZeroPriceAreRejected(){assertThatThrownBy(()->pricing.price(product("1000","100"),91)).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->pricing.price(product("0","100"),0)).isInstanceOf(IllegalArgumentException.class);}
}
