package com.aria;
import com.aria.entity.*;
import com.aria.integration.*;
import com.aria.service.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties={"aria.smart-pricing.schedule-enabled=false","aria.intelligence.external-enabled=false","aria.seed-demo-catalog=false","aria.seed-demo-users=false","aria.manager.enabled=false","aria.manager.schedule-enabled=false","aria.store-sync.worker-enabled=false","aria.integrations.encryption-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=","spring.datasource.url=${ARIA_TEST_DATABASE_URL:jdbc:h2:mem:storeprices;DB_CLOSE_DELAY=-1}","spring.datasource.username=${ARIA_TEST_DATABASE_USER:sa}","spring.datasource.password=${ARIA_TEST_DATABASE_PASSWORD:}","spring.datasource.driver-class-name=${ARIA_TEST_DATABASE_DRIVER:org.h2.Driver}","spring.flyway.enabled=${ARIA_TEST_FLYWAY:false}","spring.flyway.locations=classpath:db/migration/postgresql","spring.flyway.baseline-on-migrate=true","spring.flyway.baseline-version=0","spring.jpa.hibernate.ddl-auto=${ARIA_TEST_SCHEMA_MODE:update}","logging.level.root=ERROR"})
@Transactional
class StorePriceIntegrationTest {
 @Autowired StorePriceService prices;@Autowired ConnectedStoresService stores;@Autowired EntityManager em;@Autowired IntegrationVault vault;
 @MockitoBean WooCommerceReader woo;
 ConnectedStore store;StoreSnapshot item;final AtomicReference<String> remote=new AtomicReference<>("1500.00");
 private static BigDecimal money(String value){return new BigDecimal(value);}
 @BeforeEach void setup(){
  store=new ConnectedStore();store.organization=em.find(Organization.class,1);store.name="Изолированный магазин цен";store.baseUrl="https://store.example.invalid";store.platform="WOOCOMMERCE";store.status="CONNECTED";store.catalogCurrency="KZT";store.currencyConfirmedAt=LocalDateTime.now();store.lastSync=LocalDateTime.now();em.persist(store);em.flush();
  item=new StoreSnapshot();item.storeId=store.id;item.externalId="10";item.name="Учебный товар";item.sku="ST-10";item.currency="KZT";item.price=money("1500");item.availableQuantity=18;item.observedAt=LocalDateTime.now();em.persist(item);
  var access=new ExternalIntegration();access.store=store;access.provider="WOOCOMMERCE";access.status="READ_VERIFIED";access.encryptedCredentials=vault.encrypt("ck_"+"a".repeat(32)+"\ncs_"+"b".repeat(32));em.persist(access);em.flush();
  when(woo.currency(anyString(),anyString(),anyString())).thenReturn("KZT");
  when(woo.product(anyString(),nullable(String.class),eq("10"),anyString(),anyString())).thenAnswer(call->live());
  when(woo.setRegularPrice(anyString(),nullable(String.class),eq("10"),any(),anyString(),anyString())).thenAnswer(call->{remote.set(((BigDecimal)call.getArgument(3)).setScale(2).toPlainString());return live();});
 }
 private Map<String,Object> live(){var row=new LinkedHashMap<String,Object>();row.put("id",10);row.put("price",remote.get());row.put("regular_price",remote.get());row.put("sale_price","");row.put("date_on_sale_from_gmt",null);row.put("date_on_sale_to_gmt",null);row.put("manage_stock",true);row.put("stock_quantity",18);return row;}
 private void facts(){prices.confirmProduct(store.id,"10",0,money("750"),money("1000"),"Текстиль",true,"Подтверждено владельцем для теста");}
 private void permissions(){var p=prices.settings(store.id);prices.configure(store.id,store.version,((Number)p.get("version")).longValue(),"RECOMMEND",false,false,true,false,20,1,List.of());}
 private StorePriceChange change(String key,String target){return prices.request(store.id,store.version,"10",item.price,money(target),key,"Подтверждённое учебное изменение");}
 @Test void actualReadbackIsRequiredAndSameRequestNeverResends(){facts();permissions();var c=change("same-price-request","1350");assertThat(c.status).isEqualTo("APPLIED");assertThat(item.price).isEqualByComparingTo("1350");assertThat(item.availableQuantity).isEqualTo(18);var again=prices.request(store.id,store.version,"10",money("1500"),money("1350"),"same-price-request","Подтверждённое учебное изменение");assertThat(again.id).isEqualTo(c.id);verify(woo,times(1)).setRegularPrice(anyString(),nullable(String.class),anyString(),any(),anyString(),anyString());assertThatThrownBy(()->change("same-price-request","1300")).hasMessageContaining("другой операции");}
 @Test void writePermissionAndOwnerCostAreBothRequired(){assertThatThrownBy(()->change("without-owner-cost","1350")).hasMessageContaining("себестоимость");facts();assertThatThrownBy(()->change("without-permission","1350")).hasMessageContaining("не разрешил");verify(woo,never()).setRegularPrice(anyString(),nullable(String.class),anyString(),any(),anyString(),anyString());}
 @Test void floorAndCumulativeMaximumAreValidatedBeforeAnyRequest(){facts();permissions();assertThatThrownBy(()->change("below-cost-floor","900")).hasMessageContaining("минимума");assertThatThrownBy(()->change("below-reference-floor","1100")).hasMessageContaining("базовой цены");verifyNoInteractions(woo);}
 @Test void currencyChangeAndAnExistingCampaignNeverGetOverwritten(){facts();permissions();when(woo.currency(anyString(),anyString(),anyString())).thenReturn("USD");assertThat(change("currency-has-changed","1350").status).isEqualTo("STALE");when(woo.currency(anyString(),anyString(),anyString())).thenReturn("KZT");var sale=live();sale.put("sale_price","1400");when(woo.product(anyString(),nullable(String.class),anyString(),anyString(),anyString())).thenReturn(sale);assertThat(change("campaign-is-active","1350").status).isEqualTo("STALE");verify(woo,never()).setRegularPrice(anyString(),nullable(String.class),anyString(),any(),anyString(),anyString());}
 @Test void staleSnapshotAndUnknownStockAreNotWritable(){facts();permissions();item.observedAt=LocalDateTime.now().minusDays(2);em.flush();assertThatThrownBy(()->change("stale-snapshot-write","1350")).hasMessageContaining("Обновите каталог");item.observedAt=LocalDateTime.now();item.availableQuantity=null;em.flush();assertThatThrownBy(()->change("unknown-stock-write","1350")).hasMessageContaining("остатка");verifyNoInteractions(woo);}
 @Test void providerDenialIsNotReportedAsASuccess(){facts();permissions();when(woo.setRegularPrice(anyString(),nullable(String.class),anyString(),any(),anyString(),anyString())).thenThrow(new WooCommerceReader.WriteRejected());var c=change("read-only-access-key","1350");assertThat(c.status).isEqualTo("REJECTED");assertThat(item.price).isEqualByComparingTo("1500");assertThat(c.outcomeNote).contains("отклонил");}
 @Test void ambiguousWriteIsPersistedAndReconciledWithoutASecondPut(){facts();permissions();when(woo.setRegularPrice(anyString(),nullable(String.class),anyString(),any(),anyString(),anyString())).thenAnswer(call->{remote.set("1350.00");throw new WooCommerceReader.WriteUncertain();});var c=change("uncertain-price-write","1350");assertThat(c.status).isEqualTo("VERIFY_REQUIRED");assertThatThrownBy(()->change("second-price-request","1300")).hasMessageContaining("предыдущую операцию");c.sendingAt=LocalDateTime.now().minusSeconds(31);em.flush();assertThat(prices.reconcile(store.id,c.id).status).isEqualTo("APPLIED");assertThat(item.price).isEqualByComparingTo("1350");verify(woo,times(1)).setRegularPrice(anyString(),nullable(String.class),anyString(),any(),anyString(),anyString());}
 @Test void consentRevocationDuringPreflightPreventsThePut(){facts();permissions();when(woo.currency(anyString(),anyString(),anyString())).thenAnswer(call->{prices.stop(store.id);return "KZT";});assertThat(change("revoked-during-check","1350").status).isEqualTo("REJECTED");verify(woo,never()).setRegularPrice(anyString(),nullable(String.class),anyString(),any(),anyString(),anyString());}
 @Test void localConcurrentPriceChangeAndCooldownAreProtected(){facts();permissions();remote.set("1550.00");assertThat(change("remote-price-changed","1350").status).isEqualTo("STALE");remote.set("1500.00");assertThat(change("first-actual-change","1350").status).isEqualTo("APPLIED");assertThatThrownBy(()->change("too-frequent-change","1300")).hasMessageContaining("интервала");}
 @Test void importedCurrencyChangeInvalidatesOwnerFacts(){facts();permissions();item.currency="USD";store.catalogCurrency="USD";em.flush();assertThatThrownBy(()->change("old-cost-new-currency","1350")).hasMessageContaining("текущей валюте");verifyNoInteractions(woo);}
 @Test void archiveAndEmergencyStopDisableFurtherWrites(){facts();permissions();prices.stop(store.id);assertThatThrownBy(()->change("stopped-price-request","1350")).hasMessageContaining("не разрешил");permissions();store.archived=true;em.flush();assertThatThrownBy(()->change("archived-price-request","1350")).hasMessageContaining("активное");verifyNoInteractions(woo);}
}
