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
 @org.springframework.beans.factory.annotation.Autowired private WooCatalogImporter importer;
 @org.springframework.beans.factory.annotation.Autowired private StoreSyncRunRepository syncRuns;
 @org.springframework.beans.factory.annotation.Autowired private StoreSyncQueue syncQueue;
 @org.springframework.beans.factory.annotation.Autowired private StoreWebhookService webhooks;
 @org.springframework.beans.factory.annotation.Autowired private WooOrderImporter orderImporter;
 @org.springframework.beans.factory.annotation.Autowired private StoreOrderSnapshotRepository externalOrders;
 private final tools.jackson.databind.ObjectMapper json=tools.jackson.databind.json.JsonMapper.builder().build();
 private final ConnectedStoreRepository stores;private final ExternalIntegrationRepository integrations;private final StoreSnapshotRepository snapshots;private final ProductRepository products;private final IntegrationVault vault;private final WooCommerceReader woo;private final jakarta.persistence.EntityManager em;private final AuditService audit;
 public ConnectedStoresService(ConnectedStoreRepository s,ExternalIntegrationRepository i,StoreSnapshotRepository p,ProductRepository products,IntegrationVault v,WooCommerceReader w,jakarta.persistence.EntityManager em,AuditService a){stores=s;integrations=i;snapshots=p;this.products=products;vault=v;woo=w;this.em=em;audit=a;}
 public Object list(){return Map.of("organization",em.find(Organization.class,1),"stores",stores.findAll(org.springframework.data.domain.Sort.by("id")).stream().map(this::view).toList(),"secureCredentials",vault.available());}
 private Map<String,Object> view(ConnectedStore s){var v=new LinkedHashMap<String,Object>();v.put("id",s.id);v.put("name",s.name);v.put("platform",s.platform);v.put("url",s.baseUrl);v.put("status",s.status);v.put("version",s.version);v.put("lastSync",s.lastSync);v.put("lastError",s.lastError);v.put("tasks",syncQueue.list(s.id));v.put("orders",externalOrders.countByStoreId(s.id));v.put("lastOrdersSync",s.lastOrdersSync);v.put("lastOrdersError",s.lastOrdersError);v.put("catalogCurrency",s.platform.equals("ARIA_DEMO")?"KZT":s.catalogCurrency);v.put("currencyConfirmedAt",s.currencyConfirmedAt);v.put("currencySource",s.platform.equals("ARIA_DEMO")?"ARIA":"WooCommerce data/currencies/current");v.put("products",s.platform.equals("ARIA_DEMO")&&!s.status.equals("DISCONNECTED")?products.count():snapshots.countByStoreId(s.id));v.put("ai",s.platform.equals("ARIA_DEMO")?"Настраивается в ARIA AI":"Нужны данные о закупке и партиях");return v;}
 public Object rename(String name){var org=em.find(Organization.class,1);org.name=name.trim();audit.record("BUSINESS_RENAMED","organization:1","Название бизнеса обновлено");return list();}
 public Object create(String name,String platform,String url){
  if(!Set.of("WOOCOMMERCE","ARIA_DEMO").contains(platform))throw new IllegalArgumentException("Эта платформа пока не поддерживается");
  if(platform.equals("ARIA_DEMO")&&!em.find(Organization.class,1).demonstration)throw new IllegalArgumentException("Демонстрационный каталог доступен только в демо-компании");
  em.find(Organization.class,1,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
  if(stores.count()>=10)throw new IllegalStateException("Лимит пилота — 10 магазинов");
  if(platform.equals("ARIA_DEMO")&&stores.findAll().stream().anyMatch(s->s.platform.equals(platform)&&!s.status.equals("DISCONNECTED")))throw new IllegalArgumentException("Демонстрационный магазин уже добавлен");
  var s=new ConnectedStore();s.organization=em.find(Organization.class,1);s.name=name.trim();s.platform=platform;s.baseUrl=platform.equals("ARIA_DEMO")?"/index.html":normalize(url);s.status=platform.equals("ARIA_DEMO")?"CONNECTED":"ACTION_REQUIRED";stores.saveAndFlush(s);audit.record("STORE_CREATED","store:"+s.id,platform);return view(s);
 }
 private String normalize(String url){try{var u=java.net.URI.create(url);if(!"https".equals(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null||u.getQuery()!=null||u.getFragment()!=null)throw new Exception();return url.replaceAll("/+$","");}catch(Exception e){throw new IllegalArgumentException("Укажите адрес магазина вида https://shop.example");}}
 private ConnectedStore store(Long id){return stores.lockById(id).orElseThrow(()->new IllegalArgumentException("Магазин не найден"));}
 public Object credentials(Long id,long version,String key,String secret){
  var s=store(id);fresh(s,version);if(!s.platform.equals("WOOCOMMERCE"))throw new IllegalArgumentException("Ключи нужны только WooCommerce");em.lock(s,jakarta.persistence.LockModeType.PESSIMISTIC_FORCE_INCREMENT);syncQueue.cancelAll(id);webhooks.revoke(id);
  var i=integrations.findByStoreId(id).orElseGet(ExternalIntegration::new);i.store=s;i.provider="WOOCOMMERCE";i.encryptedCredentials=vault.encrypt(key+"\n"+secret);i.status="NOT_VERIFIED";integrations.save(i);s.status="ACTION_REQUIRED";s.lastError=null;em.flush();audit.record("STORE_CREDENTIALS_SAVED","store:"+id,"Доступы заменены; требуется проверка");return view(s);
 }
 public Object sync(Long id,long version,boolean checkOnly){
  var s=store(id);fresh(s,version);if(s.status.equals("DISCONNECTED"))throw new IllegalArgumentException("Магазин отключён. Настройте его заново");
  var run=new StoreSyncRun();run.storeId=id;run.operation=checkOnly?"CHECK":"SYNC";
  try{
   if(s.platform.equals("WOOCOMMERCE")){
    var integration=integrations.findByStoreId(id).orElseThrow(()->new IllegalArgumentException("Сначала сохраните ключи чтения"));
    var credentials=vault.decrypt(integration.encryptedCredentials).split("\n",-1);
    if(checkOnly)woo.get(s.baseUrl,"products?per_page=100&page=1",credentials[0],credentials[1]);
    else{
     var catalog=importer.read(s.baseUrl,credentials[0],credentials[1]);var observed=LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
     var previous=snapshots.findByStoreIdOrderByNameAsc(id);var ids=new HashMap<String,Long>();previous.forEach(p->ids.put(p.externalId,p.id));
     var incoming=new HashSet<String>();var result=new ArrayList<StoreSnapshot>();
     for(var p:catalog.items()){var item=new StoreSnapshot();item.id=ids.get(p.externalId());item.storeId=id;item.externalId=p.externalId();item.parentExternalId=p.parentId();item.name=p.name();item.sku=p.sku();item.price=p.price();item.availableQuantity=p.quantity();item.observedAt=observed;item.currency=catalog.currency();result.add(item);incoming.add(item.externalId);}
     snapshots.deleteAll(previous.stream().filter(p->!incoming.contains(p.externalId)).toList());snapshots.saveAll(result);s.lastSync=observed;s.catalogCurrency=catalog.currency();s.currencyConfirmedAt=observed;run.imported=result.size();run.skipped=catalog.skipped();
    }
    integration.status="READ_VERIFIED";
   }else{s.lastSync=LocalDateTime.now();}
   s.status="CONNECTED";s.lastError=null;run.status="SUCCESS";run.note=s.platform.equals("ARIA_DEMO")?"Проверен существующий демо-каталог; новые товары не импортировались":"Только чтение. Пропущено неподдерживаемых типов: "+run.skipped;
   audit.record(checkOnly?"STORE_ACCESS_CHECKED":"STORE_SYNCED","store:"+id,"Только чтение. Внешние цены и склад не менялись");
  }catch(IllegalArgumentException|IllegalStateException e){s.status="ERROR";s.lastError=e.getMessage();run.status="ERROR";run.note=s.lastError;audit.record("STORE_SYNC_FAILED","store:"+id,s.lastError);}
  run.completedAt=LocalDateTime.now();syncRuns.save(run);em.flush();return view(s);
 }
 public Object tasks(Long id){store(id);return syncQueue.list(id);}
 public Object executeQueued(StoreSyncQueue.Claim claim){
  syncQueue.requireActive(claim);
  Object result=claim.operation().equals("ORDERS")?syncOrders(claim.storeId(),claim.storeVersion()):sync(claim.storeId(),claim.storeVersion(),claim.operation().equals("CHECK"));
  syncQueue.requireActive(claim);return result;
 }
 public Object history(Long id){store(id);return syncRuns.findTop30ByStoreIdOrderByIdDesc(id);}
 public Object syncOrders(Long id,long version){
  var s=store(id);fresh(s,version);
  if(!s.platform.equals("WOOCOMMERCE")||s.status.equals("DISCONNECTED"))throw new IllegalArgumentException("Для чтения заказов настройте подключение WooCommerce");
  var run=new StoreSyncRun();run.storeId=id;run.operation="ORDERS";
  try{
   var integration=integrations.findByStoreId(id).orElseThrow(()->new IllegalArgumentException("Сначала сохраните ключи чтения"));
   var credentials=vault.decrypt(integration.encryptedCredentials).split("\n",-1);
   var incoming=orderImporter.read(s.baseUrl,credentials[0],credentials[1]);
   for(var item:incoming){var row=externalOrders.findByStoreIdAndExternalId(id,item.externalId()).orElseGet(StoreOrderSnapshot::new);
    if(row.modifiedAtUtc!=null&&row.modifiedAtUtc.isAfter(item.modifiedAtUtc())){run.skipped++;continue;}
    row.storeId=id;row.externalId=item.externalId();row.status=item.status();row.currency=item.currency();row.total=item.total();row.refundedTotal=item.refundedTotal();row.createdAtUtc=item.createdAtUtc();row.modifiedAtUtc=item.modifiedAtUtc();row.completedAtUtc=item.completedAtUtc();row.observedAt=LocalDateTime.now();row.linesJson=json.writeValueAsString(item.lines().stream().map(line->{var value=new LinkedHashMap<String,Object>();value.put("productId",line.productId());value.put("quantity",line.quantity());value.put("total",line.total().toPlainString());return value;}).toList());externalOrders.save(row);run.imported++;
   }
   s.lastOrdersSync=LocalDateTime.now();s.lastOrdersError=null;run.status="SUCCESS";run.note="Снимки заказов без контактов. Локальные заказы и остатки не изменялись. Устаревших записей пропущено: "+run.skipped;
   audit.record("STORE_ORDERS_SYNCED","store:"+id,run.note);
  }catch(IllegalArgumentException|IllegalStateException e){s.lastOrdersError=e.getMessage();run.status="ERROR";run.note=s.lastOrdersError;audit.record("STORE_ORDERS_SYNC_FAILED","store:"+id,run.note);}
  run.completedAt=LocalDateTime.now();syncRuns.save(run);em.flush();return view(s);
 }
 public Object orders(Long id){store(id);return externalOrders.findTop100ByStoreIdOrderByModifiedAtUtcDescIdDesc(id).stream().map(row->{var map=new LinkedHashMap<String,Object>();map.put("id",row.id);map.put("externalId",row.externalId);map.put("status",row.status);map.put("currency",row.currency);map.put("total",row.total.toPlainString());map.put("refundedTotal",row.refundedTotal==null?null:row.refundedTotal.toPlainString());map.put("createdAtUtc",row.createdAtUtc);map.put("modifiedAtUtc",row.modifiedAtUtc);map.put("completedAtUtc",row.completedAtUtc);map.put("observedAt",row.observedAt);map.put("lines",json.readValue(row.linesJson,List.class));return map;}).toList();}
 public Object disconnect(Long id,long version){var s=store(id);fresh(s,version);syncQueue.cancelAll(id);webhooks.revoke(id);integrations.findByStoreId(id).ifPresent(i->{i.encryptedCredentials=null;i.status="REVOKED_LOCALLY";});s.status="DISCONNECTED";s.writeEnabled=false;s.lastError=null;audit.record("STORE_DISCONNECTED","store:"+id,"Локальные ключи удалены; ключ WooCommerce также отзовите у провайдера");em.flush();return view(s);}
 public Object catalog(Long id){var s=store(id);if(s.platform.equals("ARIA_DEMO"))return products.findAll().stream().limit(100).map(p->Map.of("name",p.getName(),"sku",p.getSku(),"price",p.getUnitPrice(),"availableQuantity",p.getQuantityOnHand(),"currency","KZT")).toList();return snapshots.findByStoreIdOrderByNameAsc(id).stream().map(row->{var value=new LinkedHashMap<String,Object>();value.put("id",row.id);value.put("storeId",row.storeId);value.put("externalId",row.externalId);value.put("parentExternalId",row.parentExternalId);value.put("name",row.name);value.put("sku",row.sku);value.put("price",row.price==null?null:row.price.toPlainString());value.put("currency",row.currency);value.put("observedAt",row.observedAt);value.put("availableQuantity",row.availableQuantity);return value;}).toList();}
 private void fresh(ConnectedStore s,long version){if(s.version!=version)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"Данные изменились. Обновите список магазинов");}
}
