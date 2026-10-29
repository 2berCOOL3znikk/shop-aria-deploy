package com.aria.service;
import com.aria.entity.*;
import com.aria.repository.*;
import com.aria.controller.SmartPricingController.SettingsInput;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
@Service @Transactional
public class ManagerStore {
 private final AiManagerStateRepository states;private final ManagerRunRepository runs;private final ManagerActionRepository actions;private final SmartPricingService smart;private final AuditService audit;
 @org.springframework.beans.factory.annotation.Autowired private SmartPricingSettingsRepository pricingSettings;
 private final tools.jackson.databind.ObjectMapper json=tools.jackson.databind.json.JsonMapper.builder().enable(tools.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).build();
 public ManagerStore(AiManagerStateRepository s,ManagerRunRepository r,ManagerActionRepository a,SmartPricingService p,AuditService log){states=s;runs=r;actions=a;smart=p;audit=log;}
 private AiManagerState lock(){return states.lockState().orElseGet(()->states.saveAndFlush(new AiManagerState()));}
 public record Claim(String lease,String mode,boolean ai,boolean social,boolean purchases,long cursor){}
 public Claim claim(boolean force){
  var s=lock();var now=LocalDateTime.now();if("OFF".equals(s.mode)||s.leaseUntil!=null&&s.leaseUntil.isAfter(now)||!force&&s.dirtyAt==null&&s.nextRun!=null&&s.nextRun.isAfter(now)||s.lastStarted!=null&&s.lastStarted.isAfter(now.minusMinutes(1)))return null;
  if(s.lease!=null)runs.findByLease(s.lease).filter(r->"RUNNING".equals(r.status)).ifPresent(r->{r.status="INTERRUPTED";r.finishedAt=now;});
  s.lease=UUID.randomUUID().toString();s.leaseUntil=now.plusMinutes(5);s.lastStarted=now;s.nextRun=now.plusMinutes(s.intervalMinutes);s.dirtyAt=null;s.lastError=null;
  var run=new ManagerRun();run.lease=s.lease;run.mode=s.mode;runs.save(run);audit.record("MANAGER_CYCLE_STARTED","manager",s.mode+" · внутренний цикл, максимум 50 товаров и один AI-запрос");return new Claim(s.lease,s.mode,s.automaticAiConsent,s.socialDrafts,s.purchaseDrafts,s.productCursor);
 }
 public boolean current(String lease){var s=lock();return !"OFF".equals(s.mode)&&Objects.equals(s.lease,lease)&&s.leaseUntil!=null&&s.leaseUntil.isAfter(LocalDateTime.now());}
 public void markDirty(){var s=lock();if(!"OFF".equals(s.mode))s.dirtyAt=LocalDateTime.now();}
 public void progress(String lease,Long productId,String name){if(!current(lease))return;var s=lock();s.leaseUntil=LocalDateTime.now().plusMinutes(5);runs.findByLease(lease).ifPresent(r->r.resultJson=json.writeValueAsString(Map.of("phase","ANALYZE","productId",productId,"product",name)));}
 public void finish(Claim claim,long cursor,Map<String,Object> result){
  var s=lock();var now=LocalDateTime.now();boolean current=Objects.equals(s.lease,claim.lease());
  runs.findByLease(claim.lease()).ifPresent(r->{r.finishedAt=now;r.resultJson=json.writeValueAsString(result);r.status=current?(Boolean.TRUE.equals(result.get("failed"))?"PARTIAL":"DONE"):"CANCELLED";});
  if(current){s.lease=null;s.leaseUntil=null;s.lastFinished=now;s.productCursor=cursor;s.lastError=Boolean.TRUE.equals(result.get("failed"))?"Часть проверок не завершена. Откройте журнал цикла.":null;}
  audit.record("MANAGER_CYCLE_FINISHED","manager",json.writeValueAsString(result).substring(0,Math.min(1000,json.writeValueAsString(result).length())));
 }
 public Map<String,Object> configuration(){var s=lock();var out=new LinkedHashMap<String,Object>();out.put("mode",s.mode);out.put("revision",s.configRevision);out.put("automaticAiConsent",s.automaticAiConsent);out.put("socialDrafts",s.socialDrafts);out.put("purchaseDrafts",s.purchaseDrafts);out.put("dailyDraftBudget",s.dailyDraftBudget);out.put("intervalMinutes",s.intervalMinutes);out.put("running",s.leaseUntil!=null&&s.leaseUntil.isAfter(LocalDateTime.now()));out.put("nextRun",s.nextRun);out.put("lastStarted",s.lastStarted);out.put("lastFinished",s.lastFinished);out.put("lastError",s.lastError);return out;}
 public void configure(long revision,String mode,boolean ai,boolean social,boolean purchases,int interval,BigDecimal budget,Long pricingVersion,boolean policyConfirmed){
  var s=lock();if(s.configRevision!=revision)throw new IllegalStateException("Настройки менеджера изменились; обновите страницу");
  var p=smart.settings();if(ai&&!Boolean.TRUE.equals(p.get("externalAiEnabled")))throw new IllegalStateException("Сначала разрешите внешний AI и состав данных в настройках Smart Pricing");
  if("AUTOPILOT".equals(mode)){
   if(!ai||!policyConfirmed||!Objects.equals(pricingVersion,p.get("version")))throw new IllegalStateException("Автопилот требует согласия на автоматический AI и подтверждения актуальных границ Smart Pricing");
   smart.configure(new SettingsInput((Long)p.get("version"),"AUTOPILOT",((Number)p.get("maximumDiscount")).intValue(),((Number)p.get("cooldownHours")).intValue(),false,Boolean.TRUE.equals(p.get("allowRestore")),(List<String>)p.get("allowedCategories"),true,false,((Number)p.get("aiDailyBudget")).intValue(),null,null));
  }else {smart.stop();if("OFF".equals(mode))disableExternalAi();}
  s.mode=mode;s.automaticAiConsent=ai&&!"OFF".equals(mode);s.socialDrafts=social;s.purchaseDrafts=purchases;s.intervalMinutes=interval;s.dailyDraftBudget=budget;s.configRevision++;s.lease=null;s.leaseUntil=null;s.nextRun=null;
  audit.record("MANAGER_CONFIGURED","manager",mode+" · автоматический AI "+s.automaticAiConsent+" · черновики закупок "+purchases+" · лимит "+budget+" ₸/день");
 }
 private void disableExternalAi(){var p=pricingSettings.lockSettings().orElseThrow();p.externalAiEnabled=false;p.scheduledAiEnabled=false;p.publicAssistantAiEnabled=false;p.consentRevision++;p.updatedAt=LocalDateTime.now();pricingSettings.saveAndFlush(p);}
 public void stop(){var s=lock();s.mode="OFF";s.automaticAiConsent=false;s.configRevision++;s.lease=null;s.leaseUntil=null;smart.stop();disableExternalAi();audit.record("MANAGER_STOPPED","manager","Будущие циклы, внешний AI и ценовой автопилот отключены. Уже изменённые цены сохранены.");}
 public void stopPricing(){var s=lock();if(!"OFF".equals(s.mode))s.mode="ADVISOR";s.automaticAiConsent=false;s.configRevision++;s.lease=null;s.leaseUntil=null;smart.stop();audit.record("MANAGER_PRICING_STOPPED","manager","Автономное изменение цен остановлено; ручные AI-запросы сохраняют прежние разрешения.");}
 public void signal(String lease,Long productId,Long decisionId,String type,String severity,String title,String reason,Map<String,Object> facts){
  if(!current(lease))return;String serialized=json.writeValueAsString(facts);String fp;
  try{fp=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(serialized.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("SHA-256 unavailable");}
  String key=type+":"+productId;var a=actions.findBySignalKey(key).orElseGet(ManagerAction::new);boolean changed=!fp.equals(a.fingerprint);
  a.signalKey=key;a.type=type;a.productId=productId;a.decisionId=decisionId;a.title=title;a.reason=reason;a.severity=severity;a.factsJson=serialized;a.fingerprint=fp;a.lastSeen=LocalDateTime.now();
  if(changed){a.status="OPEN";a.resolvedAt=null;audit.record("MANAGER_RISK_FOUND","product:"+productId,type+" · "+title);}actions.save(a);
 }
 public void resolveAbsent(String lease,Long productId,Set<String> found){if(!current(lease))return;for(var a:actions.findByProductId(productId))if(!found.contains(a.type)&&!"RESOLVED".equals(a.status)){a.status="RESOLVED";a.resolvedAt=LocalDateTime.now();audit.record("MANAGER_RISK_RESOLVED","action:"+a.id,"Основание больше не обнаружено");}}
 public void acknowledge(Long id){var a=actions.findById(id).orElseThrow(()->new IllegalArgumentException("Действие не найдено"));if("OPEN".equals(a.status)){a.status="ACKNOWLEDGED";audit.record("MANAGER_ACTION_ACKNOWLEDGED","action:"+id,"Администратор ознакомился; риск не считается устранённым");}}
 @Transactional(readOnly=true) public List<ManagerRun> runs(){return runs.findTop20ByOrderByStartedAtDescIdDesc();}
 @Transactional(readOnly=true) public List<ManagerAction> actions(){return actions.findTop200ByOrderByLastSeenDescIdDesc();}
}
