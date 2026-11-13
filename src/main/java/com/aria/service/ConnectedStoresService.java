package com.aria.service;
import com.aria.entity.*;
import com.aria.repository.*;
import com.aria.integration.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.LocalDateTime;
@Service @Transactional
public class ConnectedStoresService {
 @org.springframework.beans.factory.annotation.Autowired private StoreSyncFlow syncFlow;
 @org.springframework.beans.factory.annotation.Autowired private org.springframework.transaction.PlatformTransactionManager transactionManager;
 @org.springframework.beans.factory.annotation.Autowired private StoreSyncRunRepository syncRuns;
 @org.springframework.beans.factory.annotation.Autowired private StoreSyncQueue syncQueue;
 @org.springframework.beans.factory.annotation.Autowired private StoreWebhookService webhooks;
 @org.springframework.beans.factory.annotation.Autowired private StoreOrderSnapshotRepository externalOrders;
 private final tools.jackson.databind.ObjectMapper json=tools.jackson.databind.json.JsonMapper.builder().build();
 private final ConnectedStoreRepository stores;private final ExternalIntegrationRepository integrations;private final StoreSnapshotRepository snapshots;private final ProductRepository products;private final IntegrationVault vault;private final WooCommerceReader woo;private final jakarta.persistence.EntityManager em;private final AuditService audit;
 public ConnectedStoresService(ConnectedStoreRepository s,ExternalIntegrationRepository i,StoreSnapshotRepository p,ProductRepository products,IntegrationVault v,WooCommerceReader w,jakarta.persistence.EntityManager em,AuditService a){stores=s;integrations=i;snapshots=p;this.products=products;vault=v;woo=w;this.em=em;audit=a;}
 public Object list(){return Map.of("organization",em.find(Organization.class,1),"stores",stores.findAll(org.springframework.data.domain.Sort.by("id")).stream().map(this::view).toList(),"secureCredentials",vault.available());}
 private Map<String,Object> view(ConnectedStore s){var v=new LinkedHashMap<String,Object>();v.put("id",s.id);v.put("syncInProgress",syncFlow.inProgress(s.id));v.put("name",s.name);v.put("platform",s.platform);v.put("url",s.baseUrl);v.put("status",s.status);v.put("version",s.version);v.put("writeEnabled",s.writeEnabled);v.put("archived",s.archived);v.put("archivedAt",s.archivedAt);v.put("testConnection",s.testConnection);v.put("hasCredentials",integrations.findByStoreId(s.id).map(i->i.encryptedCredentials!=null).orElse(false));v.put("lastSync",s.lastSync);v.put("lastError",s.lastError);v.put("tasks",syncQueue.list(s.id));v.put("orders",externalOrders.countByStoreId(s.id));v.put("lastOrdersSync",s.lastOrdersSync);v.put("lastOrdersError",s.lastOrdersError);v.put("catalogCurrency",s.platform.equals("ARIA_DEMO")?"KZT":s.catalogCurrency);v.put("currencyConfirmedAt",s.currencyConfirmedAt);v.put("currencySource",s.platform.equals("ARIA_DEMO")?"ARIA":"WooCommerce data/currencies/current");v.put("products",s.platform.equals("ARIA_DEMO")?products.countByArchivedFalse():snapshots.countByStoreId(s.id));v.put("ai",s.platform.equals("ARIA_DEMO")?"Настраивается в ARIA AI":"Нужны данные о закупке и партиях");return v;}
 public Object rename(String name){var org=em.find(Organization.class,1);org.name=name.trim();audit.record("BUSINESS_RENAMED","organization:1","Название бизнеса обновлено");return list();}
 public Object create(String name,String platform,String url){return create(name,platform,url,false);}
 public Object create(String name,String platform,String url,boolean testConnection){
  if(!Set.of("WOOCOMMERCE","ARIA_DEMO").contains(platform))throw new IllegalArgumentException("Эта платформа пока не поддерживается");
  if(platform.equals("ARIA_DEMO")&&!em.find(Organization.class,1).demonstration)throw new IllegalArgumentException("Демонстрационный каталог доступен только в демо-компании");
  em.find(Organization.class,1,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
  if(stores.countByArchivedFalse()>=10)throw new IllegalStateException("Лимит — 10 актуальных подключений. Архивируйте неиспользуемое подключение");
  if(stores.count()>=100)throw new IllegalStateException("Достигнут лимит истории подключений. Удалите пустые тестовые подключения");
  if(testConnection&&!platform.equals("WOOCOMMERCE"))throw new IllegalArgumentException("Учебный магазин ARIA уже обозначен отдельно");
  if(platform.equals("ARIA_DEMO")&&stores.findAll().stream().anyMatch(s->s.platform.equals(platform)&&!s.archived&&!s.status.equals("DISCONNECTED")))throw new IllegalArgumentException("Демонстрационный магазин уже добавлен");
  var s=new ConnectedStore();s.organization=em.find(Organization.class,1);s.name=name.trim();s.testConnection=testConnection;s.platform=platform;s.baseUrl=platform.equals("ARIA_DEMO")?"/index.html":normalize(url);s.status=platform.equals("ARIA_DEMO")?"CONNECTED":"ACTION_REQUIRED";stores.saveAndFlush(s);audit.record("STORE_CREATED","store:"+s.id,platform);return view(s);
 }
 private String normalize(String url){try{var u=java.net.URI.create(url);if(!"https".equals(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null||u.getQuery()!=null||u.getFragment()!=null)throw new Exception();return url.replaceAll("/+$","");}catch(Exception e){throw new IllegalArgumentException("Укажите адрес магазина вида https://shop.example");}}
 private ConnectedStore store(Long id){return stores.lockById(id).orElseThrow(()->new IllegalArgumentException("Магазин не найден"));}
 public Object credentials(Long id,long version,String key,String secret){
  var s=store(id);fresh(s,version);active(s);if(!s.platform.equals("WOOCOMMERCE"))throw new IllegalArgumentException("Ключи нужны только WooCommerce");em.lock(s,jakarta.persistence.LockModeType.PESSIMISTIC_FORCE_INCREMENT);syncQueue.cancelAll(id);webhooks.revoke(id);revokePricePermissions(id);
  var i=integrations.findByStoreId(id).orElseGet(ExternalIntegration::new);i.store=s;i.provider="WOOCOMMERCE";i.encryptedCredentials=vault.encrypt(key+"\n"+secret);i.status="NOT_VERIFIED";integrations.save(i);s.status="ACTION_REQUIRED";s.writeEnabled=false;s.lastError=null;em.flush();audit.record("STORE_CREDENTIALS_SAVED","store:"+id,"Доступы заменены; требуется проверка");return view(s);
 }
 @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
 public Object sync(Long id,long version,boolean checkOnly){syncFlow.synchronize(id,version,checkOnly?"CHECK":"SYNC",null);return currentView(id);}
 private Object currentView(Long id){var tx=new org.springframework.transaction.support.TransactionTemplate(transactionManager);tx.setReadOnly(true);return tx.execute(status->{em.clear();return view(stores.findById(id).orElseThrow(()->new IllegalArgumentException("Магазин не найден")));});}
 public Object tasks(Long id){store(id);return syncQueue.list(id);}
 @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
 public Object executeQueued(StoreSyncQueue.Claim claim){syncFlow.synchronize(claim.storeId(),claim.storeVersion(),claim.operation(),claim);return currentView(claim.storeId());}
 public Object history(Long id){store(id);return syncRuns.findTop30ByStoreIdOrderByIdDesc(id);}
 @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
 public Object syncOrders(Long id,long version){syncFlow.synchronize(id,version,"ORDERS",null);return currentView(id);}
 public Object orders(Long id){store(id);return externalOrders.findTop100ByStoreIdOrderByModifiedAtUtcDescIdDesc(id).stream().map(row->{var map=new LinkedHashMap<String,Object>();map.put("id",row.id);map.put("externalId",row.externalId);map.put("status",row.status);map.put("currency",row.currency);map.put("total",row.total.toPlainString());map.put("refundedTotal",row.refundedTotal==null?null:row.refundedTotal.toPlainString());map.put("createdAtUtc",row.createdAtUtc);map.put("modifiedAtUtc",row.modifiedAtUtc);map.put("completedAtUtc",row.completedAtUtc);map.put("observedAt",row.observedAt);map.put("lines",json.readValue(row.linesJson,List.class));return map;}).toList();}
 public Object disconnect(Long id,long version){var s=store(id);fresh(s,version);syncQueue.cancelAll(id);webhooks.revoke(id);revokePricePermissions(id);integrations.findByStoreId(id).ifPresent(i->{i.encryptedCredentials=null;i.credentialReference=null;i.status="REVOKED_LOCALLY";});s.status="DISCONNECTED";s.writeEnabled=false;s.lastError=null;audit.record("STORE_DISCONNECTED","store:"+id,"Локальные ключи удалены; ключ WooCommerce также отзовите у провайдера");em.flush();return view(s);}
 public Object catalog(Long id){var s=store(id);if(s.platform.equals("ARIA_DEMO"))return products.findAll().stream().filter(p->!p.getArchived()).limit(100).map(p->{var row=new LinkedHashMap<String,Object>();row.put("name",p.getName());row.put("sku",p.getSku());row.put("price",p.getUnitPrice());row.put("availableQuantity",p.getQuantityOnHand());row.put("currency","KZT");return row;}).toList();return snapshots.findByStoreIdOrderByNameAsc(id).stream().map(row->{var value=new LinkedHashMap<String,Object>();value.put("id",row.id);value.put("storeId",row.storeId);value.put("externalId",row.externalId);value.put("parentExternalId",row.parentExternalId);value.put("name",row.name);value.put("sku",row.sku);value.put("price",row.price==null?null:row.price.toPlainString());value.put("currency",row.currency);value.put("observedAt",row.observedAt);value.put("availableQuantity",row.availableQuantity);return value;}).toList();}
 private void revokePricePermissions(Long id){var p=em.find(StorePricingPolicy.class,id);if(p!=null){p.autonomyEnabled=false;p.engineFallbackConsent=false;p.priceWriteConsent=false;p.scheduledAiConsent=false;p.mode="RECOMMEND";p.consentRevision++;p.updatedAt=LocalDateTime.now();}}
 private void active(ConnectedStore s){if(s.archived)throw new IllegalStateException("Подключение в архиве. Сначала восстановите его");}
 public Object presentation(Long id,long version,String name,boolean testConnection,String reason){
  var s=store(id);fresh(s,version);if(testConnection&&!s.platform.equals("WOOCOMMERCE"))throw new IllegalArgumentException("Учебный магазин ARIA уже обозначен отдельно");
  s.name=name.trim();s.testConnection=testConnection;em.flush();audit.record("STORE_PRESENTATION_CHANGED","store:"+id,"Тестовое подключение: "+testConnection+" · "+reason);return view(s);
 }
 public Object archive(Long id,long version,String reason){
  var s=store(id);fresh(s,version);active(s);syncQueue.cancelAll(id);webhooks.revoke(id);revokePricePermissions(id);s.archived=true;s.archivedAt=LocalDateTime.now();s.status="DISCONNECTED";s.writeEnabled=false;em.flush();audit.record("STORE_ARCHIVED","store:"+id,reason+" · обновления и уведомления остановлены, история сохранена");return view(s);
 }
 public Object restore(Long id,long version,String reason){
  em.find(Organization.class,1,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);var s=store(id);fresh(s,version);if(!s.archived)throw new IllegalStateException("Подключение уже в актуальном списке");
  if(stores.countByArchivedFalse()>=10)throw new IllegalStateException("Лимит — 10 актуальных подключений");
  if(s.platform.equals("ARIA_DEMO")&&stores.findAll().stream().anyMatch(other->!other.id.equals(id)&&!other.archived&&other.platform.equals("ARIA_DEMO")&&!other.status.equals("DISCONNECTED")))throw new IllegalStateException("Учебный магазин уже подключён");
  s.archived=false;s.archivedAt=null;s.status=s.platform.equals("ARIA_DEMO")?"CONNECTED":"ACTION_REQUIRED";s.writeEnabled=false;em.flush();audit.record("STORE_RESTORED","store:"+id,reason+" · перед обновлением проверьте доступ; уведомления не включались");return view(s);
 }
 private List<String> removalBlockers(ConnectedStore s){
  var blockers=new ArrayList<String>();if(syncFlow.inProgress(s.id))blockers.add("Дождитесь завершения текущего чтения магазина");if(!s.testConnection||!s.platform.equals("WOOCOMMERCE"))blockers.add("Удалять можно только явно отмеченные тестовые WooCommerce-подключения");if(!s.archived)blockers.add("Сначала перенесите подключение в архив");
  var relations=Map.of("StoreSnapshot","В каталоге есть сохранённые товары","StoreOrderSnapshot","Есть сохранённые заказы","StoreSyncRun","Есть история обновлений","StoreSyncTask","Есть сохранённые задания обновления","StoreWebhookReceipt","Есть история уведомлений","StorePricingPolicy","Есть сохранённые разрешения цен","StoreProductPolicy","Есть подтверждённые данные товаров","StorePriceChange","Есть журнал внешних изменений цен","StorePricingDecision","Есть история анализа подключённого каталога");
  for(var relation:relations.entrySet())if(em.createQuery("select count(r) from "+relation.getKey()+" r where r.storeId=:id",Long.class).setParameter("id",s.id).getSingleResult()>0)blockers.add(relation.getValue());
  integrations.findByStoreId(s.id).ifPresent(i->{if(i.encryptedCredentials!=null||i.credentialReference!=null)blockers.add("Сначала отзовите локальный доступ");});return blockers;
 }
 public Object removalReadiness(Long id){var s=store(id);var blockers=removalBlockers(s);return Map.of("allowed",blockers.isEmpty(),"blockers",blockers,"version",s.version);}
 public Object removeEmptyTest(Long id,long version,String confirmation,String reason){
  var s=store(id);fresh(s,version);if(!"DELETE_EMPTY_TEST_STORE".equals(confirmation))throw new IllegalArgumentException("Подтвердите удаление пустого тестового подключения");var blockers=removalBlockers(s);if(!blockers.isEmpty())throw new IllegalStateException(String.join(". ",blockers));
  integrations.findByStoreId(id).ifPresent(integrations::delete);var receiver=em.find(StoreWebhookReceiver.class,id);if(receiver!=null)em.remove(receiver);em.flush();stores.delete(s);em.flush();audit.record("EMPTY_TEST_STORE_REMOVED","store:"+id,reason+" · зависимостей не было");return Map.of("removed",true);
 }
 private void fresh(ConnectedStore s,long version){if(s.version!=version)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"Данные изменились. Обновите список магазинов");}
}
