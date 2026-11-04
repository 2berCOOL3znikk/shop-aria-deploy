package com.aria;

import com.aria.entity.*;
import com.aria.repository.*;
import com.aria.service.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties={"aria.smart-pricing.schedule-enabled=false","aria.intelligence.external-enabled=false","aria.seed-demo-catalog=false","aria.seed-demo-users=false","aria.manager.enabled=false","aria.manager.schedule-enabled=false","aria.store-sync.worker-enabled=false","aria.integrations.encryption-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=","spring.datasource.url=${ARIA_TEST_DATABASE_URL:jdbc:h2:mem:storelifecycle;DB_CLOSE_DELAY=-1}","spring.datasource.username=${ARIA_TEST_DATABASE_USER:sa}","spring.datasource.password=${ARIA_TEST_DATABASE_PASSWORD:}","spring.datasource.driver-class-name=${ARIA_TEST_DATABASE_DRIVER:org.h2.Driver}","spring.flyway.enabled=${ARIA_TEST_FLYWAY:false}","spring.flyway.locations=classpath:db/migration/postgresql","spring.flyway.baseline-on-migrate=true","spring.flyway.baseline-version=0","spring.jpa.hibernate.ddl-auto=${ARIA_TEST_SCHEMA_MODE:update}","logging.level.root=ERROR"})
@Transactional
class StoreLifecycleIntegrationTest {
 @Autowired ConnectedStoresService service;@Autowired ConnectedStoreRepository stores;@Autowired StoreSyncQueue queue;@Autowired StoreWebhookService webhooks;@Autowired ExternalIntegrationRepository integrations;@Autowired EntityManager em;
 @SuppressWarnings("unchecked") private Map<String,Object> map(Object value){return (Map<String,Object>)value;}
 private long id(Map<String,Object> s){return ((Number)s.get("id")).longValue();}private long version(Map<String,Object> s){return ((Number)s.get("version")).longValue();}
 private Map<String,Object> create(boolean test){return map(service.create("Магазин для проверки архива","WOOCOMMERCE","https://lifecycle.example.invalid",test));}
 private StoreSnapshot snapshot(long id){var s=new StoreSnapshot();s.storeId=id;s.externalId="1";s.name="Сохранённый товар";s.price=BigDecimal.TEN;s.currency="KZT";em.persist(s);em.flush();return s;}
 @Test void archiveCancelsClaimAndWebhookButRetainsEncryptedAccessAndCatalogue(){
  var s=create(true);long id=id(s);s=map(service.credentials(id,version(s),"ck_"+"a".repeat(32),"cs_"+"b".repeat(32)));webhooks.enable(id,version(s));var current=stores.findById(id).orElseThrow();snapshot(id);queue.enqueue(id,current.version,"CHECK");var claim=queue.claim();assertThat(claim).isNotNull();assertThat(claim.storeId()).isEqualTo(id);
  var archived=map(service.archive(id,current.version,"Архивирование учебного подключения"));assertThat(archived).containsEntry("archived",true).containsEntry("status","DISCONNECTED");assertThat(archived.get("archivedAt")).isNotNull();assertThat(queue.list(id).getFirst().state).isEqualTo("CANCELLED");assertThat(map(webhooks.settings(id))).containsEntry("enabled",false);assertThat(integrations.findByStoreId(id).orElseThrow().encryptedCredentials).isNotBlank();assertThat(((List<?>)service.catalog(id))).hasSize(1);
  assertThatThrownBy(()->queue.requireActive(claim)).isInstanceOf(IllegalStateException.class);assertThatThrownBy(()->service.sync(id,version(archived),true)).isInstanceOf(IllegalStateException.class);assertThatThrownBy(()->service.syncOrders(id,version(archived))).isInstanceOf(IllegalStateException.class);
 }
 @Test void restoreRequiresFreshAccessCheckAndDoesNotReenableWebhooksOrWrites(){
  var s=create(true);long id=id(s);snapshot(id);s=map(service.archive(id,version(s),"Учебный архив"));s=map(service.restore(id,version(s),"Учебное восстановление"));assertThat(s).containsEntry("archived",false).containsEntry("status","ACTION_REQUIRED");assertThat(stores.findById(id).orElseThrow().writeEnabled).isFalse();assertThat(map(webhooks.settings(id))).containsEntry("enabled",false);assertThat(queue.list(id)).isEmpty();assertThat((List<?>)service.catalog(id)).hasSize(1);
  var product=new Product();product.setSku("ARCHIVE-"+UUID.randomUUID());product.setName("Товар сохранённого каталога");product.setCategory("Тест");product.setReceivedAt(java.time.LocalDate.now());product.setUnitPrice(BigDecimal.TEN);em.persist(product);
  var nativeStore=new ConnectedStore();nativeStore.organization=em.find(Organization.class,1);nativeStore.name="Учебный каталог";nativeStore.platform="ARIA_DEMO";nativeStore.baseUrl="/index.html";nativeStore.status="CONNECTED";em.persist(nativeStore);em.flush();
  long expected=em.createQuery("select count(p) from Product p where p.archived=false",Long.class).getSingleResult();var nativeArchive=map(service.archive(nativeStore.id,nativeStore.version,"Архив учебного каталога"));assertThat(((Number)nativeArchive.get("products")).longValue()).isEqualTo(expected);assertThat((List<?>)service.catalog(nativeStore.id)).hasSize((int)Math.min(100,expected));
 }
 @Test void onlyAnArchivedExplicitlyMarkedEmptyTestCanBePermanentlyRemoved(){
  var real=create(false);long realId=id(real);var archived=map(service.archive(realId,version(real),"Сохранить реальное подключение"));assertThat(map(service.removalReadiness(realId))).containsEntry("allowed",false);assertThatThrownBy(()->service.removeEmptyTest(realId,version(archived),"DELETE_EMPTY_TEST_STORE","Проверка защиты")).isInstanceOf(IllegalStateException.class);assertThat(stores.existsById(realId)).isTrue();
  var test=create(true);long testId=id(test);assertThatThrownBy(()->service.removeEmptyTest(testId,version(test),"DELETE_EMPTY_TEST_STORE","Проверка защиты")).isInstanceOf(IllegalStateException.class);
 }
 @Test void removingAnEmptyTestPreservesItsAudit(){
  var s=create(true);long id=id(s);s=map(service.archive(id,version(s),"Пустой учебный архив"));assertThat(map(service.removalReadiness(id))).containsEntry("allowed",true);service.removeEmptyTest(id,version(s),"DELETE_EMPTY_TEST_STORE","Подтверждённое удаление пустого теста");assertThat(stores.existsById(id)).isFalse();assertThat(em.createQuery("select count(a) from AuditEvent a where a.action='EMPTY_TEST_STORE_REMOVED' and a.resource=:resource",Long.class).setParameter("resource","store:"+id).getSingleResult()).isEqualTo(1L);
 }
 @Test void retainedProductsHistoryTasksAndCredentialsEachBlockDestructiveRemoval(){
  var withProducts=create(true);long productStore=id(withProducts);snapshot(productStore);var a=map(service.archive(productStore,version(withProducts),"Учебный архив"));assertThatThrownBy(()->service.removeEmptyTest(productStore,version(a),"DELETE_EMPTY_TEST_STORE","Проверка связей")).hasMessageContaining("товары");
  var withTask=create(true);long taskStore=id(withTask);queue.enqueue(taskStore,version(withTask),"CHECK");var b=map(service.archive(taskStore,version(withTask),"Учебный архив"));assertThatThrownBy(()->service.removeEmptyTest(taskStore,version(b),"DELETE_EMPTY_TEST_STORE","Проверка связей")).hasMessageContaining("задания");
  var withKeys=create(true);long keyStore=id(withKeys);var keyed=map(service.credentials(keyStore,version(withKeys),"ck_"+"a".repeat(32),"cs_"+"b".repeat(32)));var d=map(service.archive(keyStore,version(keyed),"Учебный архив"));assertThatThrownBy(()->service.removeEmptyTest(keyStore,version(d),"DELETE_EMPTY_TEST_STORE","Проверка связей")).hasMessageContaining("доступ");
 }
 @Test void classificationIsVersionedAndDoesNotEraseHistory(){
  var s=create(false);long id=id(s),oldVersion=version(s);snapshot(id);var changed=map(service.presentation(id,oldVersion,"Мой магазин",true,"Владелец подтвердил учебное подключение"));assertThat(changed).containsEntry("name","Мой магазин").containsEntry("testConnection",true);assertThat(version(changed)).isGreaterThan(oldVersion);assertThatThrownBy(()->service.archive(id,oldVersion,"Устаревшая форма")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);assertThat((List<?>)service.catalog(id)).hasSize(1);
 }
 @Test void archivingFreesAnActiveSlotWithoutRemovingExistingConnections(){
  var s=create(true);long id=id(s);while(stores.countByArchivedFalse()<10)create(true);assertThatThrownBy(()->create(false)).hasMessageContaining("10 актуальных");service.archive(id,version(s),"Освободить актуальный слот");var next=create(false);assertThat(next).containsEntry("archived",false);assertThat(stores.countByArchivedFalse()).isEqualTo(10);assertThat(stores.existsById(id)).isTrue();
 }
}
