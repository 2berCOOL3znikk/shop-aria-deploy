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
 private final ConnectedStoreRepository stores;private final ExternalIntegrationRepository integrations;private final StoreSnapshotRepository snapshots;private final ProductRepository products;private final IntegrationVault vault;private final WooCommerceReader woo;private final jakarta.persistence.EntityManager em;private final AuditService audit;
 public ConnectedStoresService(ConnectedStoreRepository s,ExternalIntegrationRepository i,StoreSnapshotRepository p,ProductRepository products,IntegrationVault v,WooCommerceReader w,jakarta.persistence.EntityManager em,AuditService a){stores=s;integrations=i;snapshots=p;this.products=products;vault=v;woo=w;this.em=em;audit=a;}
 public Object list(){return Map.of("organization",em.find(Organization.class,1),"stores",stores.findAll(org.springframework.data.domain.Sort.by("id")).stream().map(this::view).toList(),"secureCredentials",vault.available());}
 private Map<String,Object> view(ConnectedStore s){var v=new LinkedHashMap<String,Object>();v.put("id",s.id);v.put("name",s.name);v.put("platform",s.platform);v.put("url",s.baseUrl);v.put("status",s.status);v.put("version",s.version);v.put("lastSync",s.lastSync);v.put("lastError",s.lastError);v.put("products",s.platform.equals("ARIA_DEMO")&&!s.status.equals("DISCONNECTED")?products.count():snapshots.countByStoreId(s.id));v.put("ai",s.platform.equals("ARIA_DEMO")?"Настраивается в ARIA AI":"Нужны данные о закупке и партиях");return v;}
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
  var s=store(id);fresh(s,version);if(!s.platform.equals("WOOCOMMERCE"))throw new IllegalArgumentException("Ключи нужны только WooCommerce");em.lock(s,jakarta.persistence.LockModeType.PESSIMISTIC_FORCE_INCREMENT);
  var i=integrations.findByStoreId(id).orElseGet(ExternalIntegration::new);i.store=s;i.provider="WOOCOMMERCE";i.encryptedCredentials=vault.encrypt(key+"\n"+secret);i.status="NOT_VERIFIED";integrations.save(i);s.status="ACTION_REQUIRED";s.lastError=null;em.flush();audit.record("STORE_CREDENTIALS_SAVED","store:"+id,"Доступы заменены; требуется проверка");return view(s);
 }
 public Object sync(Long id,long version,boolean checkOnly){
  var s=store(id);fresh(s,version);if(s.status.equals("DISCONNECTED"))throw new IllegalArgumentException("Магазин отключён. Настройте его заново");
  try{
   if(s.platform.equals("WOOCOMMERCE")){
    var integration=integrations.findByStoreId(id).orElseThrow(()->new IllegalArgumentException("Сначала сохраните ключи чтения"));
    var credentials=vault.decrypt(integration.encryptedCredentials).split("\n",-1);var collected=new ArrayList<Map<String,Object>>();
    for(int page=1;page<=11;page++){var part=woo.get(s.baseUrl,"products?per_page=100&page="+page,credentials[0],credentials[1]);if(page==11&&!part.isEmpty())throw new IllegalStateException("В пилоте доступно до 1000 карточек за синхронизацию");collected.addAll(part);if(checkOnly||part.size()<100)break;}
    if(!checkOnly){
     // Stage all responses before touching saved snapshots: errors preserve the previous import.
     var result=new ArrayList<StoreSnapshot>();
     for(var p:collected){if(!"simple".equals(p.get("type")))continue;var item=new StoreSnapshot();item.storeId=id;item.externalId=String.valueOf(p.get("id"));item.name=String.valueOf(p.get("name"));item.sku=String.valueOf(p.getOrDefault("sku",""));item.price=p.get("price")==null||p.get("price").toString().isBlank()?null:new java.math.BigDecimal(p.get("price").toString());item.availableQuantity=Boolean.TRUE.equals(p.get("manage_stock"))&&p.get("stock_quantity") instanceof Number n?n.intValue():null;item.observedAt=LocalDateTime.now();result.add(item);}
     snapshots.deleteByStoreId(id);snapshots.flush();snapshots.saveAll(result);s.lastSync=LocalDateTime.now();
    }
    integration.status="READ_VERIFIED";
   }else{s.lastSync=LocalDateTime.now();}
   s.status="CONNECTED";s.lastError=null;audit.record(checkOnly?"STORE_ACCESS_CHECKED":"STORE_SYNCED","store:"+id,"Только чтение. Внешние цены и склад не менялись");
  }catch(IllegalArgumentException|IllegalStateException e){s.status="ERROR";s.lastError=e.getMessage();audit.record("STORE_SYNC_FAILED","store:"+id,s.lastError);}
  em.flush();return view(s);
 }
 public Object disconnect(Long id,long version){var s=store(id);fresh(s,version);integrations.findByStoreId(id).ifPresent(i->{i.encryptedCredentials=null;i.status="REVOKED_LOCALLY";});s.status="DISCONNECTED";s.writeEnabled=false;s.lastError=null;audit.record("STORE_DISCONNECTED","store:"+id,"Локальные ключи удалены; ключ WooCommerce также отзовите у провайдера");em.flush();return view(s);}
 public Object catalog(Long id){var s=store(id);if(s.platform.equals("ARIA_DEMO"))return products.findAll().stream().limit(100).map(p->Map.of("name",p.getName(),"sku",p.getSku(),"price",p.getUnitPrice(),"availableQuantity",p.getQuantityOnHand())).toList();return snapshots.findByStoreIdOrderByNameAsc(id);}
 private void fresh(ConnectedStore s,long version){if(s.version!=version)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"Данные изменились. Обновите список магазинов");}
}
