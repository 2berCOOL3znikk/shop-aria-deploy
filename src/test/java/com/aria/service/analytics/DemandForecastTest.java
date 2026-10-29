package com.aria.service.analytics;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class DemandForecastTest {
 @Test void noHistoryIsNotPresentedAsTrainedDemand(){var r=DemandForecast.fit(new double[90]);assertThat(r.method()).isEqualTo("INSUFFICIENT_HISTORY");assertThat(r.daily()).isEmpty();}
 @Test void shortHistoryUsesHonestBaseline(){double[] y=new double[21];java.util.Arrays.fill(y,3);var r=DemandForecast.fit(y);assertThat(r.method()).isEqualTo("MOVING_AVERAGE_7");assertThat(r.units14()).isEqualTo(42);assertThat(r.validationMae()).isNull();}
 @Test void constantHistoryDoesNotClaimMlImprovement(){double[] y=new double[90];java.util.Arrays.fill(y,5);var r=DemandForecast.fit(y);assertThat(r.method()).isEqualTo("MOVING_AVERAGE_7");assertThat(r.units14()).isEqualTo(70);assertThat(r.baselineMae()).isZero();}
 @Test void regularWeeklyPatternCanValidateAgainstMovingAverage(){double[] y=new double[90];for(int i=0;i<90;i++)y[i]=i%7<5?5:30;var copy=y.clone();var r=DemandForecast.fit(y);assertThat(r.method()).isEqualTo("VALIDATED_RIDGE_AR");assertThat(r.validationMae()).isLessThan(r.baselineMae()*.9);assertThat(r.daily()).hasSize(14).allMatch(v->Double.isFinite(v)&&v>=0);assertThat(y).isEqualTo(copy);}
 @Test void invalidObservationsFailInsteadOfInventingData(){assertThatThrownBy(()->DemandForecast.fit(new double[]{Double.NaN})).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->DemandForecast.fit(new double[]{-1})).isInstanceOf(IllegalArgumentException.class);}
}
