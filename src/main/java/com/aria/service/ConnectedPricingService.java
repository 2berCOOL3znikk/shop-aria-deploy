package com.aria.service;

import com.aria.entity.*;
import com.aria.service.pricing.SmartPricingEngine;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.*;
import java.time.*;
import java.util.*;

/** Adapter to the existing Java engine; external stock never creates local lots/sales. */
@Service
public class ConnectedPricingService {
 static long attentionCount(EntityManager em){return em.createQuery("select count(d) from StorePricingDecision d where d.status in ('NEEDS_DATA','REVIEW','STALE') and not exists(select n.id from StorePricingDecision n where n.storeId=d.storeId and n.externalId=d.externalId and n.id>d.id) and d.storeId in(select s.id from ConnectedStore s where s.archived=false and s.status<>'DISCONNECTED')",Long.class).getSingleResult();}
 private final EntityManager em;private final IntelligenceService ai;private final StorePriceService prices;private final AuditService audit;private final TransactionTemplate tx;
 private final tools.jackson.databind.ObjectMapper json=tools.jackson.databind.json.JsonMapper.builder().enable(tools.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).build();
 public ConnectedPricingService(EntityManager em,IntelligenceService ai,StorePriceService prices,AuditService audit,PlatformTransactionManager tm){this.em=em;this.ai=ai;this.prices=prices;this.audit=audit;tx=new TransactionTemplate(tm);}
 private ConnectedStore store(Long id){return Optional.ofNullable(em.find(ConnectedStore.class,id,LockModeType.PESSIMISTIC_WRITE)).orElseThrow(()->new IllegalArgumentException("Магазин не найден"));}
 private void active(ConnectedStore s){if(s.archived||!"WOOCOMMERCE".equals(s.platform)||!"CONNECTED".equals(s.status))throw new IllegalStateException("Сначала проверьте подключение и импортируйте каталог");}
 private StorePricingPolicy policy(Long id){var p=em.find(StorePricingPolicy.class,id);if(p==null){p=new StorePricingPolicy();p.storeId=id;}return p;}
 private StorePricingDecision decision(Long storeId,Long id){var d=em.find(StorePricingDecision.class,id,LockModeType.PESSIMISTIC_WRITE);if(d==null||!d.storeId.equals(storeId))throw new IllegalArgumentException("Рекомендация не найдена");return d;}
 private List<StorePricingDecision> current(Long id){return em.createQuery("select d from StorePricingDecision d where d.storeId=:id and not exists(select n.id from StorePricingDecision n where n.storeId=d.storeId and n.externalId=d.externalId and n.id>d.id) order by d.id desc",StorePricingDecision.class).setParameter("id",id).setMaxResults(1000).getResultList();}
 public List<Map<String,Object>> list(Long id){return tx.execute(t->{store(id);return current(id).stream().map(this::view).toList();});}
 private Map<String,Object> view(StorePricingDecision d){var v=new LinkedHashMap<String,Object>();v.put("id",d.id);v.put("storeId",d.storeId);v.put("externalId",d.externalId);v.put("name",d.productName);v.put("source",d.source);v.put("status",d.status);v.put("strategy",d.strategy);v.put("currentPrice",d.currentPrice);v.put("suggestedPrice",d.suggestedPrice);v.put("currency",d.currency);v.put("selectedPercent",d.selectedPercent);v.put("confidence",d.confidence);v.put("reason",d.reason);v.put("createdAt",d.createdAt);v.put("reviewAt",d.reviewAt);v.put("aiCompletedAt",d.aiCompletedAt);v.put("confirmationRequired",d.confirmationRequired);v.put("priceChangeId",d.priceChangeId);v.put("report",json.readValue(d.reportJson,Map.class));v.put("explanation",d.explanationJson==null?null:json.readValue(d.explanationJson,Map.class));return v;}
 private StoreSnapshot snapshot(Long id,String external){return em.createQuery("select s from StoreSnapshot s where s.storeId=:id and s.externalId=:external",StoreSnapshot.class).setParameter("id",id).setParameter("external",external).getResultStream().findFirst().orElseThrow(()->new IllegalStateException("Товар больше не присутствует в импортированном каталоге"));}
 private record Analysis(String fingerprint,Map<String,Object> payload,boolean blocked,String reason,String confidence,int percent,BigDecimal price,int hours){}
 private Analysis build(ConnectedStore store,StoreSnapshot item){
  var p=policy(store.id);var missing=new ArrayList<String>();var limitations=new ArrayList<String>();
  var owner=em.createQuery("select p from StoreProductPolicy p where p.storeId=:id and p.externalId=:external",StoreProductPolicy.class).setParameter("id",store.id).setParameter("external",item.externalId).getResultStream().findFirst().orElse(null);
  if(owner==null)missing.add("Подтвердите себестоимость, минимальную цену и категорию");
  else {if(owner.referencePrice==null||!Objects.equals(owner.currency,item.currency))missing.add("Подтвердите сведения владельца в текущей валюте");if(!owner.nonPerishableConfirmed)missing.add("Нет подтверждённых партий и сроков годности: автоматическая уценка скоропортящегося товара запрещена");}
  if(item.availableQuantity==null||item.availableQuantity<=0)missing.add("Нет известного положительного доступного остатка");
  if(item.price==null||item.price.signum()<=0)missing.add("Нет подтверждённой положительной цены");
  if(!Objects.equals(store.catalogCurrency,item.currency)||store.currencyConfirmedAt==null)missing.add("Валюта не подтверждена");
  if(item.observedAt==null||item.observedAt.isBefore(LocalDateTime.now().minusHours(24)))missing.add("Каталог нужно обновить");
  if(store.lastOrdersSync==null||store.lastOrdersSync.isBefore(LocalDateTime.now().minusHours(24)))missing.add("Обновите историю заказов");
  var sales=new ArrayList<SmartPricingEngine.Sale>();LocalDate earliest=null;
  for(var order:em.createQuery("select o from StoreOrderSnapshot o where o.storeId=:id order by o.id",StoreOrderSnapshot.class).setParameter("id",store.id).getResultList()){
   if(order.createdAtUtc!=null&&(earliest==null||order.createdAtUtc.toLocalDate().isBefore(earliest)))earliest=order.createdAtUtc.toLocalDate();
   if(!"completed".equals(order.status)||order.completedAtUtc==null||!Objects.equals(order.currency,item.currency)||order.refundedTotal==null||order.refundedTotal.signum()!=0)continue;
   var time=order.completedAtUtc.atOffset(ZoneOffset.UTC).atZoneSameInstant(ZoneId.of("Asia/Qyzylorda")).toLocalDateTime();
   for(var raw:json.readValue(order.linesJson,List.class))if(raw instanceof Map<?,?> line&&item.externalId.equals(Objects.toString(line.get("productId"),""))){int quantity=((Number)line.get("quantity")).intValue();if(quantity>0)sales.add(new SmartPricingEngine.Sale(time,quantity,new BigDecimal(line.get("total").toString()).divide(BigDecimal.valueOf(quantity),2,RoundingMode.HALF_UP)));}
  }
  int days=earliest==null?1:(int)Math.max(1,Math.min(30,java.time.temporal.ChronoUnit.DAYS.between(earliest,LocalDate.now())+1));
  limitations.add("Только завершённые заказы WooCommerce с известной датой и без возвратов. Кассовые продажи вне магазина не подтверждены.");
  limitations.add("Полнота удалённой истории и периоды отсутствия товара неизвестны. Возраст доступной истории — "+days+" дн.; это не доказанный спрос.");
  limitations.add("Остаток — агрегированная доступность WooCommerce, не созданная складская партия ARIA. Себестоимость и категория подтверждены владельцем отдельно.");
  limitations.add("Нет проверенной реакции на изменение цены, сопоставимых конкурентов и сезонности. Не обученная модель; низкая уверенность.");
  Map<String,Object> report;String reason;String confidence="LOW";int percent=0,hours=24;BigDecimal target=item.price;
  if(!missing.isEmpty()){
   reason="Нужно уточнить данные. Цена остаётся прежней: "+String.join("; ",missing)+".";
   var facts=new LinkedHashMap<String,Object>();facts.put("name",item.name);facts.put("sku",item.sku);facts.put("currentPrice",item.price);facts.put("quantity",item.availableQuantity);facts.put("currency",item.currency);facts.put("sales",sales);
   report=new LinkedHashMap<>();report.put("facts",facts);report.put("scenarios",item.price==null?List.of():List.of(Map.of("percent",0,"price",item.price,"method","KEEP_ONLY_MISSING_DATA")));report.put("reviewAfterHours",24);report.put("reason",reason);report.put("confidence","LOW");report.put("limitations",limitations);report.put("missingData",missing);
  }else{
   BigDecimal floor=owner.minimumPrice.max(owner.unitCost).max(owner.referencePrice.multiply(BigDecimal.valueOf(100-p.maximumDiscount)).divide(BigDecimal.valueOf(100),2,RoundingMode.CEILING));
   var facts=new SmartPricingEngine.Facts(item.id,item.sku,item.name,owner.category,LocalDate.now(),item.price,item.price,0,floor,p.maximumDiscount,item.availableQuantity,days,List.of(new SmartPricingEngine.Lot(null,item.availableQuantity,owner.unitCost,null)),sales,List.of(),limitations);
   var result=SmartPricingEngine.evaluate(facts);report=json.readValue(json.writeValueAsString(result),Map.class);reason=result.reason();percent=result.suggestedPercent();target=result.suggestedPrice();confidence=result.confidence();hours=result.reviewAfterHours();report.put("missingData",List.of());
  }
  var payload=new LinkedHashMap<String,Object>();payload.put("asOf",LocalDate.now());payload.put("source","WooCommerce imported catalogue and anonymized completed order lines");payload.put("report",report);payload.put("storeConsentRevision",p.consentRevision);payload.put("ownerFactsVersion",owner==null?null:owner.version);
  String hash;try{hash=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(json.writeValueAsString(payload).getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("Не удалось подготовить проверку данных");}
  return new Analysis(hash,payload,!missing.isEmpty(),reason,confidence,percent,target,hours);
 }
 public List<Map<String,Object>> analyze(Long id){return tx.execute(t->{var s=store(id);active(s);var existing=new HashMap<String,StorePricingDecision>();current(id).forEach(d->existing.put(d.externalId,d));
  for(var item:em.createQuery("select i from StoreSnapshot i where i.storeId=:id order by i.id",StoreSnapshot.class).setParameter("id",id).getResultList()){
   var a=build(s,item);var old=existing.get(item.externalId);if(old!=null&&old.fingerprint.equals(a.fingerprint)&&old.reviewAt.isAfter(LocalDateTime.now()))continue;if(old!=null&&Set.of("PENDING","KEEP","NEEDS_DATA").contains(old.status))old.status="SUPERSEDED";
   var d=new StorePricingDecision();d.storeId=id;d.externalId=item.externalId;d.productName=item.name;d.currentPrice=item.price;d.currency=item.currency;d.fingerprint=a.fingerprint;d.reportJson=json.writeValueAsString(a.payload);d.reason=a.reason;d.confidence=a.confidence;d.selectedPercent=a.percent;d.suggestedPrice=a.price;d.strategy=a.percent==0?"KEEP":"PRICE";d.status=a.blocked?"NEEDS_DATA":a.percent==0?"KEEP":"PENDING";d.reviewAt=LocalDateTime.now().plusHours(a.hours);em.persist(d);
  }em.flush();audit.record("STORE_CATALOGUE_ANALYZED","store:"+id,"Расчёт Java на фактическом импортированном каталоге; партии и продажи не создавались");return current(id).stream().map(this::view).toList();
 });}
 private record Claim(Long storeId,Long id,String token,long globalRevision,long storeRevision,String fingerprint,Map<String,Object> facts){}
 private Claim claim(Long storeId,Long id,boolean scheduled){return tx.execute(t->{
  var global=em.find(SmartPricingSettings.class,1L,LockModeType.PESSIMISTIC_WRITE);if(global==null||!global.externalAiEnabled)throw new IllegalStateException("Сначала разрешите внешний AI в ARIA AI");
  var store=store(storeId);active(store);var p=policy(storeId);if(!p.externalAiConsent||scheduled&&!p.scheduledAiConsent)throw new IllegalStateException("Нужно отдельное согласие на AI для этого магазина");
  var d=decision(storeId,id);if(!Set.of("PENDING","KEEP","NEEDS_DATA").contains(d.status)||!d.reviewAt.isAfter(LocalDateTime.now()))throw new IllegalStateException("Сначала пересчитайте рекомендацию");
  if("AI".equals(d.source)&&d.aiCompletedAt!=null)return null;
  if(d.aiClaim!=null&&d.aiRequestedAt.isAfter(LocalDateTime.now().minusSeconds(90)))throw new IllegalStateException("AI уже проверяет этот товар");
  var fresh=build(store,snapshot(storeId,d.externalId));if(!d.fingerprint.equals(fresh.fingerprint))throw new IllegalStateException("Данные изменились. Сначала обновите анализ");
  var report=(Map<?,?>)fresh.payload.get("report");if(((List<?>)report.get("scenarios")).isEmpty())throw new IllegalStateException("Для AI нужна подтверждённая текущая цена");
  if(global.lastAiRequestAt!=null&&global.lastAiRequestAt.isAfter(LocalDateTime.now().minusMinutes(1)))throw new IllegalStateException("Не чаще одного AI-запроса в минуту для компании");
  if(!LocalDate.now().equals(global.aiBudgetDate)){global.aiBudgetDate=LocalDate.now();global.aiBudgetUsed=0;}if(global.aiBudgetUsed>=global.aiDailyBudget)throw new IllegalStateException("Достигнут общий дневной лимит AI компании");global.aiBudgetUsed++;global.lastAiRequestAt=LocalDateTime.now();
  d.aiClaim=UUID.randomUUID().toString();d.aiRequestedAt=LocalDateTime.now();d.globalConsentRevision=global.consentRevision;d.storeConsentRevision=p.consentRevision;em.flush();return new Claim(storeId,id,d.aiClaim,global.consentRevision,p.consentRevision,d.fingerprint,fresh.payload);
 });}
 public Map<String,Object> decide(Long storeId,Long id,boolean scheduled){
  var claim=claim(storeId,id,scheduled);if(claim==null)return tx.execute(t->view(decision(storeId,id)));
  var response=ai.decidePricing(claim.facts);boolean[] automatic={false};
  var saved=tx.execute(t->{var global=em.find(SmartPricingSettings.class,1L,LockModeType.PESSIMISTIC_WRITE);var s=store(storeId);var p=policy(storeId);var d=decision(storeId,id);if(!Objects.equals(d.aiClaim,claim.token))throw new IllegalStateException("Ответ относится к предыдущей проверке");d.aiClaim=null;d.aiCompletedAt=LocalDateTime.now();
   var fresh=build(s,snapshot(storeId,d.externalId));if(s.archived||!s.status.equals("CONNECTED")||global==null||!global.externalAiEnabled||global.consentRevision!=claim.globalRevision||!p.externalAiConsent||p.consentRevision!=claim.storeRevision||!d.fingerprint.equals(fresh.fingerprint)){
    d.status="STALE";d.explanationJson=json.writeValueAsString(Map.of("mode","ENGINE","note","Ответ AI устарел или разрешение отозвано; цена не изменялась"));return view(d);
   }
   var report=(Map<?,?>)fresh.payload.get("report");Map<?,?> selected=null;Object raw=response.get("suggestedPercent");if(raw instanceof Number n&&n.doubleValue()==n.intValue())for(var scenario:(List<?>)report.get("scenarios"))if(scenario instanceof Map<?,?> row&&((Number)row.get("percent")).intValue()==n.intValue())selected=row;
   Object rawHours=response.get("reviewAfterHours");boolean hoursValid=rawHours instanceof Number n&&n.doubleValue()==n.intValue()&&n.intValue()>=1&&n.intValue()<=fresh.hours;
   if("AI".equals(response.get("mode"))&&response.get("model") instanceof String model&&!model.isBlank()&&selected!=null&&hoursValid&&response.get("requiresConfirmation") instanceof Boolean&&response.get("explanation") instanceof String explanation&&!explanation.isBlank()&&explanation.length()<=4000){
    d.source="AI";d.explanationJson=json.writeValueAsString(response);d.selectedPercent=((Number)selected.get("percent")).intValue();d.suggestedPrice=new BigDecimal(selected.get("price").toString());d.strategy=d.selectedPercent==0?"KEEP":"PRICE";d.confirmationRequired=(Boolean)response.get("requiresConfirmation");d.reason=explanation.substring(0,Math.min(2000,explanation.length()));d.reviewAt=LocalDateTime.now().plusHours(((Number)rawHours).intValue());d.status=fresh.blocked?"NEEDS_DATA":d.selectedPercent==0?"KEEP":"PENDING";
    automatic[0]=!fresh.blocked&&d.selectedPercent>0&&canAutopilot(s,p,d);audit.record("STORE_AI_DECISION","store:"+storeId+"/decision:"+id,"Реальный ответ модели · "+d.strategy+" · изменение не отправлено до проверки разрешений");
   }else{d.source="ENGINE";d.explanationJson=json.writeValueAsString(Map.of("mode","ENGINE","note",Objects.toString(response.get("note"),"Ответ AI не прошёл серверную проверку; сохранён расчёт Java")));audit.record("STORE_AI_UNAVAILABLE","store:"+storeId+"/decision:"+id,"Сохранён расчёт Java. Внешняя цена не менялась");}
   em.flush();return view(d);
  });
  if(automatic[0])try{return apply(storeId,id,true);}catch(RuntimeException e){auditFailure(storeId,id);}
  return saved;
 }
 private boolean canAutopilot(ConnectedStore s,StorePricingPolicy p,StorePricingDecision d){
  if(!"AUTOPILOT".equals(p.mode)||!p.scheduledAiConsent||!p.priceWriteConsent||!s.writeEnabled||d.confirmationRequired||!"AI".equals(d.source)||!p.allowLowConfidence&&!"MEDIUM".equals(d.confidence))return false;
  var payload=json.readValue(d.reportJson,Map.class);var report=(Map<?,?>)payload.get("report");if(!(report.get("sales") instanceof Map<?,?> sales)||((Number)sales.get("units30")).longValue()<10||((Number)sales.get("observedDays")).intValue()<14)return false;
  var owner=em.createQuery("select p from StoreProductPolicy p where p.storeId=:id and p.externalId=:external",StoreProductPolicy.class).setParameter("id",s.id).setParameter("external",d.externalId).getResultStream().findFirst().orElse(null);
  return owner!=null&&owner.nonPerishableConfirmed&&((List<?>)json.readValue(p.allowedCategoriesJson,List.class)).stream().anyMatch(c->c.toString().trim().equalsIgnoreCase(owner.category));
 }
 private void auditFailure(Long storeId,Long id){tx.executeWithoutResult(t->audit.record("STORE_AUTOPILOT_BLOCKED","store:"+storeId+"/decision:"+id,"Новая цена не подтверждена. Проверьте разрешения, актуальные данные и журнал отправки"));}
 public Map<String,Object> apply(Long storeId,Long id,boolean automatic){
  record Approved(long version,String external,BigDecimal before,BigDecimal after,String source){}
  var approved=tx.execute(t->{var s=store(storeId);active(s);var p=policy(storeId);var d=decision(storeId,id);if(!"PENDING".equals(d.status)||d.selectedPercent<=0||!d.reviewAt.isAfter(LocalDateTime.now()))throw new IllegalStateException("Нет актуальной рекомендации изменить цену");var fresh=build(s,snapshot(storeId,d.externalId));if(fresh.blocked||!d.fingerprint.equals(fresh.fingerprint))throw new IllegalStateException("Сначала уточните данные и пересчитайте цену");if(automatic&&!canAutopilot(s,p,d))throw new IllegalStateException("Автоматическое применение не разрешено");return new Approved(s.version,d.externalId,d.currentPrice,d.suggestedPrice,d.source);});
  var change=prices.requestForDecision(storeId,approved.version,approved.external,approved.before,approved.after,"decision-"+id,"Рекомендация подключённого магазина № "+id,approved.source,id,automatic);
  return tx.execute(t->{var d=decision(storeId,id);d.priceChangeId=change.id;d.status=change.status.equals("APPLIED")?"APPLIED":"REVIEW";em.flush();return view(d);});
 }
 public void reject(Long storeId,Long id,String reason){tx.executeWithoutResult(t->{store(storeId);var d=decision(storeId,id);if(!Set.of("PENDING","KEEP","NEEDS_DATA").contains(d.status))throw new IllegalStateException("Рекомендация уже обработана");d.status="REJECTED";d.aiClaim=null;audit.record("STORE_PRICE_RECOMMENDATION_REJECTED","store:"+storeId+"/decision:"+id,reason);});}
 public boolean backgroundOne(Long id){
  var allowed=tx.execute(t->{var s=store(id);var p=policy(id);var global=em.find(SmartPricingSettings.class,1L);return global!=null&&global.externalAiEnabled&&(global.lastAiRequestAt==null||global.lastAiRequestAt.isBefore(LocalDateTime.now().minusMinutes(1)))&&(!LocalDate.now().equals(global.aiBudgetDate)||global.aiBudgetUsed<global.aiDailyBudget)&&!s.archived&&s.status.equals("CONNECTED")&&p.externalAiConsent&&p.scheduledAiConsent;});if(!allowed)return false;
  var decisions=analyze(id);for(var d:decisions)if(!"AI".equals(d.get("source"))&&Set.of("PENDING","KEEP","NEEDS_DATA").contains(d.get("status"))){try{decide(id,((Number)d.get("id")).longValue(),true);}catch(RuntimeException e){auditFailure(id,((Number)d.get("id")).longValue());}return true;}return false;
 }
}
