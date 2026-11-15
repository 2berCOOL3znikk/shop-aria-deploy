package com.aria;

import com.aria.entity.*;
import com.aria.repository.*;
import com.aria.service.*;
import com.aria.service.integrations.PosAdapter.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties={"aria.organization.key=demo","aria.demo-catalog-import-enabled=true","aria.demo-supermarket.enabled=true","aria.seed-demo-catalog=false","aria.seed-demo-users=false","aria.intelligence.external-enabled=false","aria.smart-pricing.schedule-enabled=false","aria.manager.enabled=false","aria.manager.schedule-enabled=false","aria.store-sync.worker-enabled=false","spring.datasource.url=${ARIA_TEST_DATABASE_URL:jdbc:h2:mem:supermarket;DB_CLOSE_DELAY=-1}","spring.datasource.username=${ARIA_TEST_DATABASE_USER:sa}","spring.datasource.password=${ARIA_TEST_DATABASE_PASSWORD:}","spring.datasource.driver-class-name=${ARIA_TEST_DATABASE_DRIVER:org.h2.Driver}","spring.flyway.enabled=${ARIA_TEST_FLYWAY:false}","spring.flyway.locations=classpath:db/migration/postgresql","spring.flyway.baseline-on-migrate=true","spring.flyway.baseline-version=0","spring.jpa.hibernate.ddl-auto=${ARIA_TEST_SCHEMA_MODE:update}","logging.level.root=ERROR"})
@Transactional
class SupermarketDemoIntegrationTest {
 @Autowired SupermarketDemoService demo;@Autowired SupermarketDemoCatalogue catalogue;@Autowired ProductRepository products;@Autowired ProductDetailRepository details;@Autowired OrderRepository orders;@Autowired InventoryService inventory;@Autowired InventoryLotRepository lots;@Autowired PosService pos;@Autowired SmartPricingService pricing;@Autowired EntityManager em;@Autowired SocialProposalRepository socialProposals;
 @Test void teachingHistoryBalancesAndReimportNeverRestocksOrRepeatsTransactions(){
  var result=demo.populate(true);assertThat(result).containsEntry("created",64).containsEntry("salesReceipts",28).containsEntry("returns",1).containsEntry("demonstration",true);assertThat(products.count()).isEqualTo(80);assertThat(orders.count()).isEqualTo(29);assertThat(((Number)result.get("socialDrafts")).intValue()).isPositive();assertThat(socialProposals.findAll()).allMatch(p->p.status.equals("PROPOSED")&&p.offerId==null&&p.organizationId==null);assertThat(details.findAll()).allMatch(ProductDetail::getDemonstration);
  for(var item:catalogue.items()){
   var product=products.findBySkuIgnoreCase(item.code()).orElseThrow();int remaining=lots.findByProductId(product.getId()).stream().mapToInt(InventoryLot::getQuantity).sum();int returnUnit=item.code().equals("ARIA-LAB-057")?1:0;assertThat(remaining).as(item.code()+" recorded balance").isEqualTo(item.quantity()-returnUnit);
   assertThat(inventory.available(product)).as(item.code()+" safe availability").isEqualTo(Set.of("DAMAGED","EXPIRED").contains(item.profile())?0:item.quantity()-returnUnit);
   if(item.profile().equals("EXPIRING")||item.profile().equals("SOCIAL"))assertThat(lots.findByProductId(product.getId()).stream().filter(l->l.getQuantity()>0).map(InventoryLot::getExpiryDate).distinct()).hasSize(2);
  }
  var soap=products.findBySkuIgnoreCase("ARIA-LAB-057").orElseThrow();pos.record(new Sale("test-sale-after-demo-import",List.of(new Line(soap.getId(),1,soap.getUnitPrice()))),"test operator");int remaining=inventory.available(soap);long orderCount=orders.count();assertThat(demo.populate(true)).containsEntry("alreadyPrepared",true);assertThat(inventory.available(soap)).isEqualTo(remaining);assertThat(orders.count()).isEqualTo(orderCount);assertThat(products.count()).isEqualTo(80);
  assertThat(pricing.settings()).containsEntry("externalAiEnabled",false).containsEntry("mode","RECOMMEND");
 }
 @Test void ordinaryCompanyCannotReceiveSyntheticTradingData(){
  var company=em.find(Organization.class,1);company.demonstration=false;em.flush();assertThatThrownBy(()->demo.populate(true)).hasMessageContaining("отдельной демонстрационной компании");assertThat(products.count()).isZero();assertThat(orders.count()).isZero();
 }
}
