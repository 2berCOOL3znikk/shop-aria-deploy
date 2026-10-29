package com.aria.service;
import com.aria.repository.*;
import com.aria.entity.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import jakarta.annotation.PreDestroy;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
@Service
public class AiManagerService {
 private final ManagerStore store;private final SmartPricingService smart;private final AiDecisionService ai;private final ProductRepository products;
 private final DemandAnalyticsService analytics;private final InventoryLotRepository lots;private final SocialProposalService social;private final CharityEligibilityService charities;
 private final PurchaseService purchases;
 @org.springframework.beans.factory.annotation.Autowired private ManagerQueue queue;
 @org.springframework.beans.factory.annotation.Autowired private SocialProposalRepository proposalRows;
 @Value("${aria.manager.enabled:true}") private boolean enabled;
 @Value("${aria.manager.schedule-enabled:true}") private boolean scheduled;
 @Value("${aria.manager.activity-enabled:true}") private boolean activity;
 private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"aria-manager");t.setDaemon(true);return t;});
 private final AtomicBoolean queued=new AtomicBoolean();private ScheduledFuture<?> followUpTask;private long followUpAt=Long.MAX_VALUE,followUpGeneration;private final AtomicLong nextActivity=new AtomicLong();
 public AiManagerService(ManagerStore s,SmartPricingService p,AiDecisionService a,ProductRepository products,DemandAnalyticsService d,InventoryLotRepository l,SocialProposalService social,CharityEligibilityService c,PurchaseService purchases){store=s;smart=p;ai=a;this.products=products;analytics=d;lots=l;this.social=social;charities=c;this.purchases=purchases;}
 public void activity(){if(!enabled||!activity)return;long now=System.currentTimeMillis(),next=nextActivity.get();if(now<next){scheduleFollowUp(next-now);return;}if(nextActivity.compareAndSet(next,now+60000))enqueue(false);}
 private synchronized void scheduleFollowUp(long delay){
  long wait=Math.max(1000,delay),at=System.currentTimeMillis()+wait;
  if(followUpTask!=null&&!followUpTask.isDone()&&followUpAt<=at)return;
  if(followUpTask!=null)followUpTask.cancel(false);long generation=++followUpGeneration;followUpAt=at;
  try{followUpTask=worker.schedule(()->{synchronized(this){if(generation!=followUpGeneration)return;followUpTask=null;followUpAt=Long.MAX_VALUE;}activity();},wait,TimeUnit.MILLISECONDS);}catch(RejectedExecutionException e){followUpTask=null;followUpAt=Long.MAX_VALUE;}
 }
 @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
 public void catchUp(){if(enabled){store.markDirty();enqueue(false);}}
 public void activity(boolean changed){if(!enabled||!activity)return;if(changed)store.markDirty();activity();}
 @Scheduled(fixedDelay=60000,initialDelay=60000) public void scheduled(){if(enabled&&scheduled)enqueue(false);}
 public boolean enqueue(boolean force){if(!enabled||!queued.compareAndSet(false,true))return false;try{worker.submit(()->{try{runCycle(force);}catch(Exception e){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Manager cycle failed ({})",e.getClass().getSimpleName());}finally{queued.set(false);if(!"OFF".equals(store.configuration().get("mode"))){var due=queue.nextPending();if(due!=null)scheduleFollowUp(Math.max(60000,Duration.between(LocalDateTime.now(),due).toMillis()));}}});return true;}catch(RejectedExecutionException e){queued.set(false);return false;}}
 public Map<String,Object> status(){var result=new LinkedHashMap<String,Object>();result.put("queue",queue.dashboard());result.put("settings",store.configuration());result.put("pricing",smart.settings());result.put("ai",ai.status());result.put("actions",store.actions());result.put("runs",store.runs());result.put("queued",queued.get());result.put("enabled",enabled);result.put("scheduledAvailable",scheduled);result.put("activityAvailable",activity);result.put("costNote","Разрешены только бесплатные модели и нулевые цены провайдеров. Показаны лимиты попыток; счёт оплаты OpenRouter не подключён.");result.put("availabilityNote",scheduled?"Цикл по расписанию выполняется, пока процесс работает. Простой и рестарты компенсируются следующим запуском.":"На Free нет постоянно активного планировщика. Цикл догоняет проверки при реальном обращении к API магазина; во время сна сервера действий нет. Искусственных пингов нет.");return result;}
 public Map<String,Object> runCycle(boolean force){
  if(!enabled)return Map.of("skipped","DISABLED");var claim=store.claim(force||!queue.due().isEmpty());if(claim==null)return Map.of("skipped","OFF_NOT_DUE_OR_LEASED");
  int scanned=0,aiCalls=0,priceChanges=0,socialDrafts=0;long cursor=claim.cursor();List<String> errors=new ArrayList<>();List<Map<String,Object>> aiResults=new ArrayList<>();
  try{
   var statuses=List.of(com.aria.enums.ProductStatus.ACTIVE,com.aria.enums.ProductStatus.CANDIDATE,com.aria.enums.ProductStatus.DISCOUNTED,com.aria.enums.ProductStatus.SOCIAL,com.aria.enums.ProductStatus.RESERVED);
   var page=org.springframework.data.domain.PageRequest.of(0,50);
   var batch=products.findByArchivedFalseAndStatusInAndIdGreaterThanOrderByIdAsc(statuses,claim.cursor(),page);if(batch.isEmpty())batch=products.findByArchivedFalseAndStatusInAndIdGreaterThanOrderByIdAsc(statuses,0L,page);
   for(var product:batch)queue.ensure(product.getId(),"PERIODIC_RECONCILIATION",false);
   for(var taskId:queue.due()){
    if(!store.current(claim.lease()))break;var task=queue.claim(taskId,claim.lease());if(task==null)continue;
    var p=products.findById(task.productId()).orElseThrow();store.progress(claim.lease(),p.getId(),p.getName());cursor=p.getId();Set<String> found=new HashSet<>();boolean purchaseCreated=false,socialCreated=false,attemptedAi=false,aiDeferred=false;
    try{
     if(p.getArchived()||Set.of(com.aria.enums.ProductStatus.TRANSFERRED,com.aria.enums.ProductStatus.WRITTEN_OFF).contains(p.getStatus())){store.resolveAbsent(claim.lease(),p.getId(),Set.of());queue.complete(task,LocalDateTime.now().plusYears(1),"ENGINE DECISION","CLOSED",null,false,false,false,"Товар архивирован, передан или списан. Открытые сигналы закрыты.");continue;}
     var facts=analytics.product(p.getId());int stock=((Number)facts.get("stock")).intValue();int reorder=((Number)facts.get("reorderUnits")).intValue();
     if(((Number)facts.get("handlingRejectedUnits")).longValue()>0||((Number)facts.get("handlingUnconfirmedUnits")).longValue()>0){found.add("STORAGE");store.signal(claim.lease(),p.getId(),null,"STORAGE","CRITICAL",p.getName()+": проверка хранения","Партии с отклонённой пригодностью заблокированы. Заявленные, но не подтверждённые условия требуют проверки сотрудником.",Map.of("rejected",facts.get("handlingRejectedUnits"),"unconfirmed",facts.get("handlingUnconfirmedUnits")));}
     if(Set.of(com.aria.enums.ProductStatus.SOCIAL,com.aria.enums.ProductStatus.RESERVED).contains(p.getStatus())){
      found.add("SOCIAL");store.signal(claim.lease(),p.getId(),null,"SOCIAL","WARNING",p.getName()+": ожидается решение по социальной полке","Текущий статус "+p.getStatus()+". Подтвердите пригодность и фактическую выдачу отдельно; цена и резерв не изменяются.",Map.of("status",p.getStatus()));
      if(((Number)facts.get("expiredUnits")).longValue()>0){found.add("EXPIRED");store.signal(claim.lease(),p.getId(),null,"EXPIRED","CRITICAL",p.getName()+": истёк срок в социальном процессе","Выдача просроченного товара запрещена. Проверьте резерв и оформите допустимое завершение.",Map.of("expiredUnits",facts.get("expiredUnits")));}
      store.resolveAbsent(claim.lease(),p.getId(),found);queue.complete(task,LocalDateTime.now().plusMinutes(((Number)store.configuration().get("intervalMinutes")).intValue()),"ENGINE DECISION","SOCIAL_FOLLOWUP",null,false,false,false,"Проверен действующий социальный процесс без выдачи и списания");scanned++;continue;
     }
     if(reorder>0){found.add("REORDER");var draft=claim.purchases()?purchases.prepare(claim.lease(),p.getId(),reorder,(Double)facts.get("daysOfCoverEstimate")):Map.<String,Object>of("status","NOT_ENABLED");purchaseCreated="DRAFT".equals(draft.get("status"));store.signal(claim.lease(),p.getId(),null,"REORDER","WARNING",p.getName()+": проверить пополнение",facts.get("reorderBasis")+". Внутренняя закупка: "+purchaseStatus(String.valueOf(draft.get("status")))+". Не отправляется поставщику автоматически.",Map.of("units",reorder,"stock",stock,"purchase",draft));}
     if(((Number)facts.get("expiredUnits")).longValue()>0){found.add("EXPIRED");store.signal(claim.lease(),p.getId(),null,"EXPIRED","CRITICAL",p.getName()+": просроченные партии","Продажа и передача просроченных партий запрещены. Требуется физическая проверка и оформленное списание.",Map.of("units",facts.get("expiredUnits"),"cost",facts.get("expiredCost")));}
     if(!"NONE".equals(facts.get("anomaly"))){found.add("ANOMALY");store.signal(claim.lease(),p.getId(),null,"ANOMALY","WARNING",p.getName()+": изменилась частота отгрузок","Сравнение соседних 7-дневных периодов, не доказательство причины. Проверьте наличие, цену и продажи.",Map.of("changePercent",facts.get("changePercent"),"units7",facts.get("units7"),"previousUnits7",facts.get("previousUnits7")));}
     var d=smart.analyzeForManager(p.getId());scanned++;
     boolean wasPending="PENDING".equals(d.get("status"));
     boolean needsAi=d.get("aiCompletedAt")==null||"ENGINE".equals(d.get("source"))&&((LocalDateTime)d.get("aiCompletedAt")).isBefore(LocalDateTime.now().minusHours(1));
     if(claim.ai()&&needsAi&&aiCalls==0&&wasPending&&stock>0&&store.current(claim.lease())){
      var cfg=smart.settings();if(Boolean.TRUE.equals(cfg.get("externalAiEnabled"))&&((Number)cfg.get("aiBudgetUsed")).intValue()<((Number)cfg.get("aiDailyBudget")).intValue()){
       try{boolean hadSocial=proposalRows.findByDecisionId(((Number)d.get("id")).longValue()).isPresent();aiCalls++;attemptedAi=true;d=ai.decideForManager(((Number)d.get("id")).longValue(),"AUTOPILOT".equals(claim.mode()),claim.lease());socialCreated=!hadSocial&&proposalRows.findByDecisionId(((Number)d.get("id")).longValue()).isPresent();aiResults.add(Map.of("decisionId",d.get("id"),"productId",p.getId(),"source",d.get("source"),"status",d.get("status")));}catch(RuntimeException e){aiDeferred=true;errors.add("AI: "+e.getClass().getSimpleName());}
      }
     }
     if(wasPending&&"APPLIED".equals(d.get("status"))&&"AI".equals(d.get("source")))priceChanges++;
     var report=(Map<?,?>)d.get("report");var scenarios=(List<?>)report.get("scenarios");long id=((Number)d.get("id")).longValue();
     if("PENDING".equals(d.get("status"))&&((Number)report.get("suggestedPercent")).intValue()!=((Number)((Map<?,?>)report.get("facts")).get("currentPercent")).intValue()){
      found.add("PRICING");store.signal(claim.lease(),p.getId(),id,"PRICING","INFO",p.getName()+": ценовое решение",String.valueOf(report.get("reason")),Map.of("decisionId",id,"source",d.get("source"),"status",d.get("status")));
     }
     boolean risk=stock>0&&!scenarios.isEmpty()&&scenarios.stream().allMatch(s->((Number)((Map<?,?>)s).get("estimatedExpiredUnits")).doubleValue()>0);
     if(risk&&"PENDING".equals(d.get("status"))){found.add("SOCIAL");int matches=charities.matching(p).size();store.signal(claim.lease(),p.getId(),id,"SOCIAL","WARNING",p.getName()+": риск непроданного остатка","Оценка сценариев: риск остаётся при всех допустимых ценах. Подходящих организаций по зарегистрированным условиям: "+matches+". Пригодность, регион и перевозку подтверждает сотрудник.",Map.of("decisionId",id,"stock",stock,"matchingOrganizations",matches));
      if(claim.social()&&store.current(claim.lease())){try{boolean existed=proposalRows.findByDecisionId(id).isPresent();social.proposeForManager(id,claim.lease());socialDrafts++;socialCreated|=!existed;}catch(RuntimeException e){errors.add("Социальный черновик "+p.getId()+": "+e.getClass().getSimpleName());}}
     }
     store.resolveAbsent(claim.lease(),p.getId(),found);
     boolean applied=wasPending&&"APPLIED".equals(d.get("status"))&&"AI".equals(d.get("source"));
     String source="AI".equals(d.get("source"))?"AI DECISION":attemptedAi?"AI UNAVAILABLE":"ENGINE DECISION";
     var nextReview=(LocalDateTime)d.get("reviewAt");if(attemptedAi&&"ENGINE".equals(d.get("source"))){var retry=aiDeferred?LocalDateTime.now().plusMinutes(2):LocalDateTime.now().plusHours(1);if(retry.isBefore(nextReview))nextReview=retry;}
     queue.complete(task,nextReview,source,String.valueOf(d.get("strategy")),id,applied,purchaseCreated,socialCreated,String.valueOf(report.get("reason"))+(attemptedAi&&"ENGINE".equals(d.get("source"))?" Внешний AI не дал проверенного решения; сохранён ENGINE.":!attemptedAi&&"AI".equals(d.get("source"))?" Использовано сохранённое AI-решение, нового запроса не было.":""));
    }catch(RuntimeException e){String message=e.getClass().getSimpleName()+((e instanceof IllegalStateException||e instanceof IllegalArgumentException)&&e.getMessage()!=null?": "+e.getMessage():"");errors.add("Товар "+p.getId()+": "+message);queue.fail(task,message);}
   }
  }catch(RuntimeException e){errors.add("Цикл: "+e.getClass().getSimpleName());}
  var result=new LinkedHashMap<String,Object>();result.put("scanned",scanned);result.put("aiAttempts",aiCalls);result.put("aiResults",aiResults);result.put("aiAppliedDecisions",priceChanges);result.put("socialDraftChecks",socialDrafts);result.put("failed",!errors.isEmpty());result.put("errors",errors.stream().limit(20).toList());store.finish(claim,cursor,result);return result;
 }
 private String purchaseStatus(String status){return switch(status){case "NOT_ENABLED"->"черновики отключены";case "DRAFT_BUDGET"->"лимит черновиков не позволяет закупку";case "NO_VERIFIED_QUOTE"->"нет проверенного предложения";case "DRAFT"->"создан черновик";case "EXISTS"->"уже есть открытый заказ";default->"требует проверки";};}
 public void stop(){store.stop();}
 public void stopPricing(){store.stopPricing();enqueue(false);}
 @PreDestroy void shutdown(){worker.shutdownNow();}
}
