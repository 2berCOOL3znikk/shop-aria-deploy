package com.aria.service;
import com.aria.service.integrations.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;
class PosterAdapterTest {
 @Test void mappedDraftIsNeverReportedAsSentOrFiscal(){var sale=new PosAdapter.Sale("qa-request-key",List.of(new PosAdapter.Line(1L,2,new BigDecimal("950"))));var plan=new PosterPosAdapter().prepare(sale,Map.of(1L,"123"));assertThat(plan.sent()).isFalse();assertThat(plan.fiscal()).isFalse();assertThat(plan.currency()).isEqualTo("KZT");assertThat(plan.lines().getFirst().externalProductId()).isEqualTo("123");}
 @Test void missingMappingFailsBeforeAnyExternalAction(){var sale=new PosAdapter.Sale("qa-request-key",List.of(new PosAdapter.Line(1L,2,BigDecimal.TEN)));assertThatThrownBy(()->new PosterPosAdapter().prepare(sale,Map.of())).isInstanceOf(IllegalArgumentException.class);}
}
