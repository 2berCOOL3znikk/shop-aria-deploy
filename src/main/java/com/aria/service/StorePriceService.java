package com.aria.service;

import com.aria.entity.*;
import com.aria.integration.*;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.*;
import java.time.*;
import java.util.*;

/** Persist-before-send workflow. An ambiguous PUT is reconciled, never blindly retried. */
@Service
public class StorePriceService {
 private final EntityManager em;private final WooCommerceReader woo;private final IntegrationVault vault;private final AuditService audit;private final TransactionTemplate tx;
 private final tools.jackson.databind.ObjectMapper json=tools.jackson.databind.json.JsonMapper.builder().build();
 public StorePriceService(EntityManager em,WooCommerceReader woo,IntegrationVault vault,AuditService audit,PlatformTransactionManager tm){this.em=em;this.woo=woo;this.vault=vault;this.audit=audit;tx=new TransactionTemplate(tm);}
 private String actor(){var a=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();return a==null?"ARIA":a.getName();}
 private ConnectedStore store(Long id){return Optional.ofNullable(em.find(ConnectedStore.class,id,LockModeType.PESSIMISTIC_WRITE)).orElseThrow(()->new IllegalArgumentException("Магазин не найден"));}
 private void active(ConnectedStore s){if(s.archived||!"CONNECTED".equals(s.status)||!"WOOCOMMERCE".equals(s.platform))throw new IllegalStateException("Проверьте активное подключение WooCommerce");}
 private StorePricingPolicy policy(Long id){var p=em.find(StorePricingPolicy.class,id);if(p==null){p=new StorePricingPolicy();p.storeId=id;p.updatedBy=actor();em.persist(p);em.flush();}return p;}
 private StoreSnapshot snapshot(Long id,String external){return em.createQuery("select s from StoreSnapshot s where s.storeId=:store and s.externalId=:external",StoreSnapshot.class).setParameter("store",id).setParameter("external",external).getResultStream().findFirst().orElseThrow(()->new IllegalArgumentException("Сначала импортируйте этот товар"));}
 private StoreProductPolicy productPolicy(Long id,String external){return em.createQuery("select p from StoreProductPolicy p where p.storeId=:store and p.externalId=:external",StoreProductPolicy.class).setParameter("store",id).setParameter("external",external).getResultStream().findFirst().orElseThrow(()->new IllegalStateException("Укажите подтверждённую себестоимость и минимальную цену товара"));}
 public Map<String,Object> settings(Long id){return tx.execute(t->{store(id);var p=em.find(StorePricingPolicy.class,id);if(p!=null)return settingsView(p);var result=settingsView(new StorePricingPolicy());result.put("storeId",id);return result;});}
 private Map<String,Object> settingsView(StorePricingPolicy p){var v=new LinkedHashMap<String,Object>();v.put("storeId",p.storeId);v.put("version",p.version);v.put("mode",p.mode);v.put("externalAiConsent",p.externalAiConsent);v.put("scheduledAiConsent",p.scheduledAiConsent);v.put("priceWriteConsent",p.priceWriteConsent);v.put("maximumDiscount",p.maximumDiscount);v.put("cooldownHours",p.cooldownHours);v.put("allowLowConfidence",p.allowLowConfidence);v.put("allowedCategories",json.readValue(p.allowedCategoriesJson,List.class));v.put("consentRevision",p.consentRevision);return v;}
 public Map<String,Object> configure(Long id,long storeVersion,long policyVersion,String mode,boolean ai,boolean scheduled,boolean writes,boolean low,int maximum,int cooldown,List<String> categories){return tx.execute(t->{
  var s=store(id);active(s);var p=policy(id);if(s.version!=storeVersion||p.version!=policyVersion)throw new IllegalStateException("Настройки изменились. Обновите магазин");
  if(!Set.of("RECOMMEND","AUTOPILOT").contains(mode)||maximum<0||maximum>50||cooldown<1||cooldown>720||categories==null||categories.size()>30||categories.stream().anyMatch(c->c==null||c.isBlank()||c.length()>200))throw new IllegalArgumentException("Проверьте границы автоматизации");
  if(scheduled&&!ai||mode.equals("AUTOPILOT")&&(!ai||!scheduled||!writes||categories.isEmpty()))throw new IllegalArgumentException("Автопилот требует отдельных разрешений AI, расписания и записи цен, а также списка категорий");
  p.mode=mode;p.externalAiConsent=ai;p.scheduledAiConsent=scheduled;p.priceWriteConsent=writes;p.allowLowConfidence=low;p.maximumDiscount=maximum;p.cooldownHours=cooldown;p.allowedCategoriesJson=json.writeValueAsString(categories.stream().map(String::trim).distinct().toList());p.consentRevision++;p.updatedAt=LocalDateTime.now();p.updatedBy=actor();s.writeEnabled=writes;em.flush();audit.record("STORE_PRICE_PERMISSIONS_CHANGED","store:"+id,"Режим "+mode+" · AI "+ai+" · расписание "+scheduled+" · запись цен "+writes+" · максимум "+maximum+"% · подтверждено владельцем");return settingsView(p);
 });}
 public Map<String,Object> stop(Long id){return tx.execute(t->{var s=store(id);var p=policy(id);p.mode="RECOMMEND";p.scheduledAiConsent=false;p.priceWriteConsent=false;p.consentRevision++;p.updatedAt=LocalDateTime.now();p.updatedBy=actor();s.writeEnabled=false;audit.record("STORE_PRICE_EMERGENCY_STOP","store:"+id,"Новые записи запрещены. Уже отправленные запросы требуют сверки результата");em.flush();return settingsView(p);});}
 public StoreProductPolicy confirmProduct(Long id,String external,long expectedVersion,BigDecimal cost,BigDecimal minimum,String category,boolean nonPerishable,String reason){return tx.execute(t->{
  active(store(id));var imported=snapshot(id,external);if(imported.price==null||imported.price.signum()<=0||imported.currency==null)throw new IllegalStateException("Сначала подтвердите цену и валюту импортом каталога");StoreProductPolicy p;
  try{p=productPolicy(id,external);}catch(IllegalStateException missing){p=new StoreProductPolicy();p.storeId=id;p.externalId=external;}
  if(p.version!=expectedVersion)throw new IllegalStateException("Характеристики изменились. Обновите товар");
  if(cost==null||cost.signum()<0||cost.scale()>2||minimum==null||minimum.signum()<=0||minimum.scale()>2||minimum.compareTo(cost)<0||category==null||category.isBlank()||category.length()>200||reason==null||reason.isBlank()||reason.length()>900)throw new IllegalArgumentException("Подтвердите себестоимость, минимум не ниже себестоимости и категорию");
  p.unitCost=cost;p.minimumPrice=minimum;p.currency=imported.currency;p.referencePrice=imported.price;p.category=category.trim();p.nonPerishableConfirmed=nonPerishable;p.confirmedAt=LocalDateTime.now();p.confirmedBy=actor();p.reason=reason;em.persist(p);em.flush();audit.record("STORE_PRODUCT_FACTS_CONFIRMED","store:"+id+"/product:"+external,"Сведения владельца в "+p.currency+"; не импортированные партии · "+reason);return p;
 });}
 public Object productFacts(Long id){return tx.execute(t->{store(id);return em.createQuery("select p from StoreProductPolicy p where p.storeId=:id",StoreProductPolicy.class).setParameter("id",id).getResultList();});}
 public List<StorePriceChange> history(Long id){return tx.execute(t->{store(id);return em.createQuery("select c from StorePriceChange c where c.storeId=:id order by c.id desc",StorePriceChange.class).setParameter("id",id).setMaxResults(100).getResultList();});}
 public StorePriceChange request(Long id,long storeVersion,String external,BigDecimal expected,BigDecimal target,String key,String reason){
  Long change=tx.execute(t->{
   var s=store(id);var existing=em.createQuery("select c from StorePriceChange c where c.storeId=:id and c.requestKey=:key",StorePriceChange.class).setParameter("id",id).setParameter("key",key).getResultStream().findFirst();
   if(existing.isPresent()){var c=existing.get();if(!c.externalId.equals(external)||c.expectedPrice.compareTo(expected)!=0||c.targetPrice.compareTo(target)!=0||!c.reason.equals(reason))throw new IllegalStateException("Идентификатор запроса уже использован для другой операции");return c.id;}
   if(s.version!=storeVersion)throw new IllegalStateException("Магазин изменился. Обновите подключение");var p=policy(id);var item=snapshot(id,external);var facts=productPolicy(id,external);validate(s,p,item,facts,target);
   if(expected==null||item.price==null||item.price.compareTo(expected)!=0||expected.compareTo(target)==0)throw new IllegalStateException("Цена изменилась или совпадает с новой. Обновите каталог");
   if(key==null||!key.matches("[A-Za-z0-9_-]{10,80}")||reason==null||reason.isBlank()||reason.length()>900)throw new IllegalArgumentException("Нужны подтверждение и причина изменения");
   var c=new StorePriceChange();c.storeId=id;c.externalId=external;c.parentExternalId=item.parentExternalId;c.productName=item.name;c.requestKey=key;c.storeVersion=s.version;c.policyRevision=p.consentRevision;c.productPolicyVersion=facts.version;c.expectedPrice=expected;c.targetPrice=target;c.currency=item.currency;c.reason=reason;c.requestedBy=actor();em.persist(c);em.flush();audit.record("STORE_PRICE_APPROVED","store:"+id+"/change:"+c.id,"Подтверждено владельцем: "+expected+" → "+target+" "+item.currency);return c.id;
  });return execute(change);
 }
 private void validate(ConnectedStore s,StorePricingPolicy p,StoreSnapshot item,StoreProductPolicy facts,BigDecimal target){
  active(s);if(!s.writeEnabled||!p.priceWriteConsent)throw new IllegalStateException("Владелец ещё не разрешил запись цен в этот магазин");
  if(item.observedAt==null||item.observedAt.isBefore(LocalDateTime.now().minusHours(24))||s.currencyConfirmedAt==null||s.currencyConfirmedAt.isBefore(LocalDateTime.now().minusHours(24))||!Objects.equals(s.catalogCurrency,item.currency))throw new IllegalStateException("Обновите каталог и подтвердите валюту перед изменением");
  if(item.availableQuantity==null||item.availableQuantity<=0)throw new IllegalStateException("Нет подтверждённого доступного остатка");
  if(facts.referencePrice==null||!Objects.equals(facts.currency,item.currency))throw new IllegalStateException("Заново подтвердите себестоимость, базовую цену и минимум в текущей валюте");
  if(target==null||target.signum()<=0||target.scale()>2||target.precision()>18||target.compareTo(facts.minimumPrice)<0||target.compareTo(facts.unitCost)<0)throw new IllegalArgumentException("Цена должна быть не ниже подтверждённой себестоимости и минимума");
  if(target.compareTo(facts.referencePrice.multiply(BigDecimal.valueOf(100-p.maximumDiscount)).divide(BigDecimal.valueOf(100),2,RoundingMode.CEILING))<0)throw new IllegalArgumentException("Цена ниже разрешённой скидки от подтверждённой базовой цены");
  var latest=em.createQuery("select c from StorePriceChange c where c.storeId=:store and c.externalId=:external and c.status in ('APPLIED','SENDING','VERIFY_REQUIRED') order by c.id desc",StorePriceChange.class).setParameter("store",s.id).setParameter("external",item.externalId).setMaxResults(1).getResultStream().findFirst();
  if(latest.isPresent()){var c=latest.get();if(!c.status.equals("APPLIED")||c.completedAt.isAfter(LocalDateTime.now().minusHours(p.cooldownHours)))throw new IllegalStateException("Сначала сверьте предыдущую операцию или дождитесь интервала между изменениями");}
 }
 private record Send(Long id,Long storeId,String address,String external,String parent,BigDecimal expected,BigDecimal target,String currency,String key,String secret,int maximum,long revision,long productVersion){}
 private StorePriceChange load(Long id){return Optional.ofNullable(em.find(StorePriceChange.class,id,LockModeType.PESSIMISTIC_WRITE)).orElseThrow(()->new IllegalArgumentException("Операция не найдена"));}
 private Send claim(Long id){return tx.execute(t->{var c=load(id);var s=store(c.storeId);if(!c.status.equals("DRAFT"))return null;var p=policy(s.id);var facts=productPolicy(s.id,c.externalId);if(c.storeVersion!=s.version||c.policyRevision!=p.consentRevision||c.productPolicyVersion!=facts.version)throw new IllegalStateException("Разрешения или сведения изменились. Создайте новую рекомендацию");validate(s,p,snapshot(s.id,c.externalId),facts,c.targetPrice);
  var integration=em.createQuery("select i from ExternalIntegration i where i.store.id=:id",ExternalIntegration.class).setParameter("id",s.id).getSingleResult();var credentials=vault.decrypt(integration.encryptedCredentials).split("\n",-1);if(credentials.length!=2)throw new IllegalStateException("Обновите защищённые доступы магазина");c.status="SENDING";c.sendingAt=LocalDateTime.now();em.flush();return new Send(c.id,s.id,s.baseUrl,c.externalId,c.parentExternalId,c.expectedPrice,c.targetPrice,c.currency,credentials[0],credentials[1],p.maximumDiscount,p.consentRevision,facts.version);
 });}
 private StorePriceChange execute(Long id){
  var send=claim(id);if(send==null)return tx.execute(t->load(id));boolean attempted=false;
  try{
   if(!woo.currency(send.address,send.key,send.secret).equals(send.currency))return finish(id,"STALE","Валюта магазина изменилась. Запись не отправлялась",null);
   var live=woo.product(send.address,send.parent,send.external,send.key,send.secret);var current=money(live,"price");var regular=money(live,"regular_price");
   if(!current.equals(send.expected.setScale(2))||!regular.equals(current)||!Objects.toString(live.get("sale_price"),"").isBlank()||live.get("date_on_sale_from_gmt")!=null||live.get("date_on_sale_to_gmt")!=null)return finish(id,"STALE","Цена или акция в WooCommerce изменилась. Обновите каталог; существующие акции не перезаписываются",null);
   if(send.target.compareTo(regular)>0||send.target.compareTo(regular.multiply(BigDecimal.valueOf(100-send.maximum)).divide(BigDecimal.valueOf(100),2,RoundingMode.CEILING))<0)return finish(id,"REJECTED","Изменение выходит за разрешённый размер скидки. Запись не отправлялась",null);
   tx.executeWithoutResult(t->{var s=store(send.storeId);var p=policy(s.id);var facts=productPolicy(s.id,send.external);active(s);if(!s.writeEnabled||!p.priceWriteConsent||p.consentRevision!=send.revision||facts.version!=send.productVersion)throw new IllegalStateException("Разрешение отозвано перед отправкой");});
   attempted=true;woo.setRegularPrice(send.address,send.parent,send.external,send.target,send.key,send.secret);
   var observed=woo.product(send.address,send.parent,send.external,send.key,send.secret);
   if(!woo.currency(send.address,send.key,send.secret).equals(send.currency)||money(observed,"price").compareTo(send.target)!=0||money(observed,"regular_price").compareTo(send.target)!=0)return finish(id,"VERIFY_REQUIRED","Магазин не подтвердил итоговую цену. Повторная отправка запрещена; сверьте результат",null);
   return finish(id,"APPLIED","Новая цена повторно прочитана из WooCommerce. Остатки и заказы этим запросом не изменялись",observed);
  }catch(RuntimeException failure){boolean uncertain=attempted&&!(failure instanceof WooCommerceReader.WriteRejected);return finish(id,uncertain?"VERIFY_REQUIRED":"REJECTED",uncertain?"Результат отправки не подтверждён. Сначала сверьте цену; автоматический повтор запрещён":attempted?"WooCommerce отклонил запись. Проверьте права ключа и настройки магазина":"Доступ или ограничения не подтверждены. Запись не отправлялась",null);}
 }
 private BigDecimal money(Map<String,Object> row,String field){try{var value=new BigDecimal(Objects.toString(row.get(field),""));if(value.signum()<0||value.scale()>2||value.precision()>18)throw new NumberFormatException();return value.setScale(2);}catch(Exception e){throw new IllegalStateException("Цена в WooCommerce не подтверждена");}}
 private StorePriceChange finish(Long id,String status,String note,Map<String,Object> observed){return tx.execute(t->{var c=load(id);store(c.storeId);c.status=status;c.outcomeNote=note;c.completedAt=LocalDateTime.now();if(observed!=null){var row=snapshot(c.storeId,c.externalId);row.price=money(observed,"price");row.observedAt=c.completedAt;}audit.record("STORE_PRICE_"+status,"store:"+c.storeId+"/change:"+id,c.source+" · "+note);em.flush();return c;});}
 public StorePriceChange reconcile(Long storeId,Long id){
  var state=tx.execute(t->{var s=store(storeId);active(s);var c=load(id);if(!c.storeId.equals(storeId))throw new IllegalArgumentException("Операция не найдена");if(!Set.of("SENDING","VERIFY_REQUIRED").contains(c.status)||c.sendingAt.isAfter(LocalDateTime.now().minusSeconds(30)))throw new IllegalStateException("Сверка доступна для неподтверждённого запроса после 30 секунд");var i=em.createQuery("select i from ExternalIntegration i where i.store.id=:id",ExternalIntegration.class).setParameter("id",storeId).getSingleResult();var credentials=vault.decrypt(i.encryptedCredentials).split("\n",-1);return new Send(c.id,s.id,s.baseUrl,c.externalId,c.parentExternalId,c.expectedPrice,c.targetPrice,c.currency,credentials[0],credentials[1],0,0,0);});
  var live=woo.product(state.address,state.parent,state.external,state.key,state.secret);if(!woo.currency(state.address,state.key,state.secret).equals(state.currency))return finish(id,"VERIFY_REQUIRED","Валюта изменилась: нужна ручная сверка в WooCommerce",null);
  var price=money(live,"price");if(price.compareTo(state.target)==0&&money(live,"regular_price").compareTo(state.target)==0)return finish(id,"APPLIED","Сверка подтвердила целевую цену. Дополнительная запись не отправлялась; авторство внешнего изменения не устанавливается",live);
  return finish(id,"FAILED","Сверка не подтвердила целевую цену. Дополнительная запись не отправлялась. Обновите каталог перед новой рекомендацией",null);
 }
}
