package com.aria;

import com.aria.entity.PricingDecision;
import com.aria.entity.Product;
import com.aria.repository.*;
import com.aria.service.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"aria.smart-pricing.schedule-enabled=false","aria.intelligence.external-enabled=false","aria.seed-demo-catalog=false","aria.seed-demo-users=false","aria.manager.enabled=false","aria.manager.schedule-enabled=false","aria.store-sync.worker-enabled=false","spring.datasource.url=${ARIA_TEST_DATABASE_URL:jdbc:h2:mem:centeractivitytest;DB_CLOSE_DELAY=-1}","spring.datasource.username=${ARIA_TEST_DATABASE_USER:sa}","spring.datasource.password=${ARIA_TEST_DATABASE_PASSWORD:}","spring.datasource.driver-class-name=${ARIA_TEST_DATABASE_DRIVER:org.h2.Driver}","spring.flyway.enabled=${ARIA_TEST_FLYWAY:false}","spring.flyway.locations=classpath:db/migration/postgresql","spring.flyway.baseline-on-migrate=true","spring.flyway.baseline-version=0","spring.jpa.hibernate.ddl-auto=${ARIA_TEST_SCHEMA_MODE:update}","logging.level.root=ERROR"})
class ControlCenterActivityTest {
    @Autowired EntityManager em;
    @Autowired PricingDecisionRepository decisions;

    @Test @Transactional
    void newlyCompletedAiAnswerSortsAheadOfEightNewerReportsAndUsesLatestTimestampWithStableTies() {
        var now = LocalDateTime.now(ZoneId.of("Asia/Qyzylorda")).minusSeconds(1);
        var product = new Product("ACTIVITY-"+UUID.randomUUID(),"Товар для проверки порядка решений","Тест",0,now.toLocalDate(),0,BigDecimal.ONE,BigDecimal.TEN,null);
        em.persist(product);
        var ai = row(product.getId(),now.minusDays(2)); ai.source="AI"; ai.aiCompletedAt=now; em.persist(ai);
        var fixture = new HashSet<Long>(); fixture.add(ai.id);
        var reports = new ArrayList<PricingDecision>();
        for(int i=0;i<8;i++) {var d=row(product.getId(),now.minusDays(1).plusMinutes(i));em.persist(d);fixture.add(d.id);reports.add(d);}
        var olderCompletion=row(product.getId(),now.minusHours(1));olderCompletion.aiCompletedAt=now.minusDays(3);em.persist(olderCompletion);fixture.add(olderCompletion.id);
        var firstTie=row(product.getId(),now.minusHours(2));em.persist(firstTie);fixture.add(firstTie.id);
        var secondTie=row(product.getId(),now.minusHours(2));em.persist(secondTie);fixture.add(secondTie.id);
        em.flush();em.clear();

        // Other test suites may share the explicitly configured test database. Compare this
        // transaction's fixtures within the real global query instead of deleting their history.
        var ordered=decisions.findRecentActivity(PageRequest.of(0,10000)).stream().filter(d->fixture.contains(d.id)).map(d->d.id).toList();
        var expected=new ArrayList<Long>(List.of(ai.id,olderCompletion.id,secondTie.id,firstTie.id));
        for(int i=reports.size()-1;i>=0;i--)expected.add(reports.get(i).id);
        assertThat(ordered).containsExactlyElementsOf(expected);
        assertThat(ordered.subList(0,8)).contains(ai.id);
    }

    @Test void centerUsesRecentActivityWindowAndPreservesVerifiedAiSource() {
        var orderRows=mock(OrderRepository.class);var stock=mock(InventoryLotRepository.class);
        var ledger=mock(InventoryLedgerService.class);var repository=mock(PricingDecisionRepository.class);
        var smart=mock(SmartPricingService.class);var actions=mock(ManagerActionRepository.class);
        when(orderRows.recordedRevenue(any())).thenReturn(Collections.singletonList(new Object[]{BigDecimal.ZERO,0L}));
        when(stock.heldValue()).thenReturn(Collections.singletonList(new Object[]{BigDecimal.ZERO,0L}));
        when(ledger.report(null,0,1)).thenReturn(Map.of("writtenOffUnits30",0L,"recordedWriteOffCost30",BigDecimal.ZERO,"unknownCostUnits30",0L));
        var d=row(1L,LocalDateTime.now().minusDays(2));d.id=91L;d.source="AI";d.aiCompletedAt=LocalDateTime.now().minusSeconds(1);d.strategy="KEEP";
        when(repository.findRecentActivity(any(Pageable.class))).thenReturn(List.of(d));
        when(smart.history(1L)).thenReturn(List.of());
        var center=new ControlCenterService(orderRows,stock,ledger,repository,smart,actions,mock(ProductRepository.class));
        org.springframework.test.util.ReflectionTestUtils.setField(center,"em",em);
        var result=center.center();
        var cards=(List<Map<String,Object>>)result.get("decisions");
        assertThat(cards).hasSize(1);
        assertThat(cards.getFirst()).containsEntry("id",91L).containsEntry("source","AI").containsEntry("strategy","KEEP");
        verify(repository).findRecentActivity(argThat(page->page.getPageNumber()==0&&page.getPageSize()==8));
        verify(repository,never()).findAllByOrderByCreatedAtDescIdDesc(any());
    }

    private PricingDecision row(Long product,LocalDateTime created) {
        var d=new PricingDecision();d.productId=product;d.status="SUPERSEDED";d.createdAt=created;d.reviewAt=created.plusHours(24);d.fingerprint=UUID.randomUUID().toString();
        d.reportJson="{\"facts\":{\"name\":\"Проверка порядка решений\"},\"suggestedPercent\":0,\"reason\":\"Учебная запись\",\"confidence\":\"LOW\"}";
        return d;
    }
}
