package com.aria;

import com.aria.dto.ControlRequests.ProductInput;
import com.aria.entity.*;
import com.aria.repository.*;
import com.aria.service.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties={"aria.demo-catalog-import-enabled=true","aria.smart-pricing.schedule-enabled=false","aria.intelligence.external-enabled=false","aria.seed-demo-catalog=false","aria.seed-demo-users=false","aria.manager.enabled=false","aria.manager.schedule-enabled=false","aria.store-sync.worker-enabled=false","spring.datasource.url=${ARIA_TEST_DATABASE_URL:jdbc:h2:mem:democataloguetest;DB_CLOSE_DELAY=-1}","spring.datasource.username=${ARIA_TEST_DATABASE_USER:sa}","spring.datasource.password=${ARIA_TEST_DATABASE_PASSWORD:}","spring.datasource.driver-class-name=${ARIA_TEST_DATABASE_DRIVER:org.h2.Driver}","spring.flyway.enabled=${ARIA_TEST_FLYWAY:false}","spring.flyway.locations=classpath:db/migration/postgresql","spring.flyway.baseline-on-migrate=true","spring.flyway.baseline-version=0","spring.jpa.hibernate.ddl-auto=${ARIA_TEST_SCHEMA_MODE:update}","logging.level.root=ERROR"})
@Transactional
class DemoCatalogueIntegrationTest {
    @Autowired DemoCatalogueImportService importer;
    @Autowired ProductRepository products;@Autowired ProductDetailRepository details;@Autowired InventoryLotRepository lots;
    @Autowired InventoryService inventory;@Autowired ControlService control;@Autowired PublicProductView view;@Autowired EntityManager em;

    @Test void importRequiresConfirmationAndAnExplicitlyEnabledDemonstrationCompany(){
        long before=products.count();assertThatThrownBy(()->importer.populate(false)).isInstanceOf(IllegalArgumentException.class);
        var organization=em.find(Organization.class,1);organization.demonstration=false;
        assertThat(importer.status()).containsEntry("available",false);
        assertThatThrownBy(()->importer.populate(true)).hasMessageContaining("не разрешён");
        assertThat(products.count()).isEqualTo(before);
    }
    @Test void disabledCapabilityCannotBeEnabledByTheRequest(){
        ReflectionTestUtils.setField(importer,"enabled",false);
        try{assertThat(importer.status()).containsEntry("available",false);assertThatThrownBy(()->importer.populate(true)).hasMessageContaining("не разрешён");}
        finally{ReflectionTestUtils.setField(importer,"enabled",true);}
    }
    @Test void repeatImportKeepsIdsPricesStockAndSourceSeparation(){
        long before=products.count();var first=importer.populate(true);assertThat((List<?>)first.get("created")).hasSize(16);assertThat(products.count()).isEqualTo(before+16);
        var p=products.findBySkuIgnoreCase("ARIA-DEMO-001").orElseThrow();var d=details.findByProductId(p.getId()).orElseThrow();assertThat(d.getDemonstration()).isTrue();assertThat(d.getCatalogueReference()).isNotBlank();
        var lot=lots.findByProductId(p.getId()).getFirst();assertThat(lot.getLotNumber()).isEqualTo("DEMO-CATALOG-V1");lot.setQuantity(5);lots.saveAndFlush(lot);inventory.synchronize(p);p.setUnitPrice(new BigDecimal("11111.00"));products.saveAndFlush(p);
        var second=importer.populate(true);assertThat((List<?>)second.get("created")).isEmpty();assertThat((List<?>)second.get("preserved")).hasSize(16);assertThat(products.count()).isEqualTo(before+16);
        assertThat(products.findBySkuIgnoreCase("ARIA-DEMO-001").orElseThrow().getId()).isEqualTo(p.getId());assertThat(inventory.available(p)).isEqualTo(5);assertThat(p.getUnitPrice()).isEqualByComparingTo("11111.00");
        assertThat(view.summary(p)).doesNotContainKeys("unitCost","minimumPrice");assertThat(((java.util.Map<?,?>)view.summary(p).get("provenance")).containsKey("facts")).isTrue();
        assertThat(p.getSalesLast30Days()).isZero();assertThat(p.getBarcode()).isNull();assertThat(d.getWeightKg()).isNull();
    }
    @Test void collidingBusinessSkuIsNeverOverwrittenOrGivenDemoProvenance(){
        var existing=control.saveProduct(null,new ProductInput("ARIA-DEMO-001","Карточка владельца","Прочее",new BigDecimal("2000"),null,null,null,null,"шт",0,null));
        long before=products.count();assertThatThrownBy(()->importer.populate(true)).hasMessageContaining("артикулов уже занят");
        assertThat(products.count()).isEqualTo(before);assertThat(existing.getName()).isEqualTo("Карточка владельца");assertThat(details.findByProductId(existing.getId()).orElseThrow().getDemonstration()).isFalse();
    }
}
