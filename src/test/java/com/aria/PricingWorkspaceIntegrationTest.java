package com.aria;

import com.aria.dto.ControlRequests.*;
import com.aria.entity.*;
import com.aria.repository.*;
import com.aria.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties={"aria.smart-pricing.schedule-enabled=false","aria.intelligence.external-enabled=false","aria.seed-demo-catalog=false","aria.seed-demo-users=false","aria.manager.enabled=false","aria.manager.schedule-enabled=false","aria.store-sync.worker-enabled=false","spring.datasource.url=${ARIA_TEST_DATABASE_URL:jdbc:h2:mem:pricingworkspacetest;DB_CLOSE_DELAY=-1}","spring.datasource.username=${ARIA_TEST_DATABASE_USER:sa}","spring.datasource.password=${ARIA_TEST_DATABASE_PASSWORD:}","spring.datasource.driver-class-name=${ARIA_TEST_DATABASE_DRIVER:org.h2.Driver}","spring.flyway.enabled=${ARIA_TEST_FLYWAY:false}","spring.flyway.locations=classpath:db/migration/postgresql","spring.flyway.baseline-on-migrate=true","spring.flyway.baseline-version=0","spring.jpa.hibernate.ddl-auto=${ARIA_TEST_SCHEMA_MODE:update}","logging.level.root=ERROR"})
@Transactional
class PricingWorkspaceIntegrationTest {
    @Autowired ControlService control;@Autowired InventoryService inventory;@Autowired SmartPricingService smart;@Autowired PricingWorkspaceService workspace;@Autowired ProductRepository products;@Autowired PricingDecisionRepository decisions;
    private Product product(String sku){var p=control.saveProduct(null,new ProductInput(sku,"Товар для проверки цен","Тесты",new BigDecimal("1000"),null,null,null,null,"шт",0,null));inventory.save(null,new LotInput(p.getId(),null,"PRICING-WORKSPACE",LocalDate.now(),null,LocalDate.now().plusDays(90),12,new BigDecimal("600")));return p;}
    @SuppressWarnings("unchecked") private List<Map<String,Object>> rows(Map<String,Object> result){return (List<Map<String,Object>>)result.get("content");}
    @Test void filtersCountAcrossPagesAndUseOnlyTheLatestDecisionForEachProduct(){
        String key="PW-"+UUID.randomUUID();var p=product(key);var first=smart.analyze(p.getId());
        p=products.findById(p.getId()).orElseThrow();p.setName("Понятное имя владельца");p.setUnitPrice(new BigDecimal("1200"));products.saveAndFlush(p);
        var stale=workspace.list("ATTENTION","Понятное имя владельца",0,10);assertThat(rows(stale)).hasSize(1);assertThat(rows(stale).getFirst()).containsEntry("stale",true);
        var live=(Map<?,?>)rows(stale).getFirst().get("live");assertThat(live.containsValue("Понятное имя владельца")).isTrue();assertThat((BigDecimal)live.get("price")).isEqualByComparingTo("1200.00");
        var latest=smart.analyze(p.getId());assertThat(latest.get("id")).isNotEqualTo(first.get("id"));
        var list=workspace.list("ALL",key,0,1);assertThat(((Number)list.get("totalElements")).longValue()).isEqualTo(1);assertThat(rows(list)).hasSize(1);assertThat(rows(list).getFirst().get("id")).isEqualTo(latest.get("id"));
        assertThat(rows(workspace.list("ALL",key,1,1))).isEmpty();assertThat(rows(workspace.list("UNCHANGED",key,0,10))).hasSize(1);
    }
    @Test void anUnreadableSavedReportDoesNotHideOtherProductsOrTheErrorFilter(){
        String key="BROKEN-PW-"+UUID.randomUUID();var p=product(key);var result=smart.analyze(p.getId());var d=decisions.findById(((Number)result.get("id")).longValue()).orElseThrow();d.reportJson="invalid-json";decisions.saveAndFlush(d);
        var list=workspace.list("ERROR",key,0,10);assertThat(rows(list)).hasSize(1);assertThat(rows(list).getFirst()).containsEntry("readError",true);assertThat(((Number)((Map<?,?>)list.get("counts")).get("ERROR")).longValue()).isEqualTo(1);
    }
    @Test void invalidFiltersAndOversizedSearchAreRejectedBeforeQuerying(){assertThatThrownBy(()->workspace.list("UNKNOWN","",0,10)).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->workspace.list("ALL","x".repeat(201),0,10)).isInstanceOf(IllegalArgumentException.class);}
}
