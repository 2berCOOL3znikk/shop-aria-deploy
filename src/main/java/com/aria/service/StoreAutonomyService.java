package com.aria.service;

import com.aria.entity.*;
import com.aria.repository.*;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.*;

/** One durable store task: bounded external reads, analysis and verified writes, without a long SQL transaction. */
@Service
public class StoreAutonomyService {
    private final EntityManager em;
    private final StoreSyncQueue queue;
    private final StoreSyncTaskRepository tasks;
    private final StoreSyncFlow flow;
    private final ConnectedPricingService pricing;
    private final StorePriceService prices;
    private final AuditService audit;
    private final TransactionTemplate tx;
    private final boolean workerAvailable;
    public StoreAutonomyService(EntityManager em,StoreSyncQueue queue,StoreSyncTaskRepository tasks,StoreSyncFlow flow,ConnectedPricingService pricing,StorePriceService prices,AuditService audit,PlatformTransactionManager tm,@Value("${aria.store-sync.worker-enabled:true}") boolean workerAvailable){
        this.em=em;this.queue=queue;this.tasks=tasks;this.flow=flow;this.pricing=pricing;this.prices=prices;this.audit=audit;this.tx=new TransactionTemplate(tm);this.workerAvailable=workerAvailable;
    }
    public record Outcome(int analyzed,int missing,int applied,long cursor,String note){}
    private ConnectedStore store(Long id){var s=em.find(ConnectedStore.class,id,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);if(s==null)throw new IllegalArgumentException("Магазин не найден");return s;}
    private StorePricingPolicy policy(Long id){return em.find(StorePricingPolicy.class,id);}
    private void require(StoreSyncQueue.Claim claim){var s=store(claim.storeId());var p=policy(s.id);if(p==null||!p.autonomyEnabled||s.archived||s.status.equals("DISCONNECTED"))throw new IllegalStateException("Автоматическая проверка остановлена");queue.requireActive(claim);}
    public void recover(){if(!workerAvailable)return;tx.executeWithoutResult(t->{for(var id:em.createQuery("select p.storeId from StorePricingPolicy p where p.autonomyEnabled=true order by p.storeId",Long.class).setMaxResults(10).getResultList())queue.requestCycle(id,false);});}
    public boolean nudge(Long id){return tx.execute(t->{var s=store(id);var p=policy(s.id);if(p==null||!p.autonomyEnabled)return false;queue.requestCycle(id,true);return true;});}
    private void phase(StoreSyncQueue.Claim claim,String operation){
        long version=tx.execute(t->{require(claim);queue.renew(claim);return store(claim.storeId()).version;});
        flow.synchronize(claim.storeId(),version,operation,claim);
        tx.executeWithoutResult(t->{require(claim);var s=store(claim.storeId());if((operation.equals("ORDERS")?s.lastOrdersError:s.lastError)!=null)throw new IllegalStateException("Магазин не подтвердил обновление. Прежние данные сохранены");});
    }
    public Outcome run(StoreSyncQueue.Claim claim){
        phase(claim,"SYNC");phase(claim,"ORDERS");
        var pending=tx.execute(t->{require(claim);return em.createQuery("select c.id from StorePriceChange c where c.storeId=:id and c.status in ('SENDING','VERIFY_REQUIRED') and c.sendingAt<:before order by c.id",Long.class).setParameter("id",claim.storeId()).setParameter("before",LocalDateTime.now().minusSeconds(30)).setMaxResults(3).getResultList();});
        for(var id:pending){tx.executeWithoutResult(t->{require(claim);queue.renew(claim);});prices.reconcile(claim.storeId(),id);}
        long cursor=tx.execute(t->{require(claim);queue.renew(claim);return tasks.findById(claim.id()).orElseThrow().cycleCursor;});
        var result=pricing.autonomousBatch(claim,cursor);
        return new Outcome(result.analyzed(),result.missing(),result.applied(),result.cursor(),"Каталог и заказы обновлены; проверено "+result.analyzed()+", подтверждённых изменений "+result.applied()+". Источники решений сохранены отдельно.");
    }
    public void finish(StoreSyncQueue.Claim claim,Outcome result,String error){tx.executeWithoutResult(t->{
        store(claim.storeId());if(!queue.finish(claim,error))return;var task=tasks.findById(claim.id()).orElseThrow();var p=policy(claim.storeId());
        var run=new StoreSyncRun();run.storeId=claim.storeId();run.operation="AUTONOMY";run.status=error==null?"SUCCESS":"ERROR";run.imported=result==null?0:result.analyzed();run.skipped=result==null?0:result.missing();run.actionsApplied=result==null?0:result.applied();run.note=error==null?result.note():error;run.completedAt=LocalDateTime.now();em.persist(run);
        if(p==null||!p.autonomyEnabled){task.state="CANCELLED";return;}
        if(error==null){task.cycleCursor=result.cursor();task.state="QUEUED";task.generation++;task.attempts=0;task.dueAt=LocalDateTime.now().plusMinutes(p.cycleIntervalMinutes);task.lastError=null;}
        audit.record(error==null?"STORE_AUTONOMY_COMPLETED":"STORE_AUTONOMY_DEFERRED","store:"+claim.storeId(),error==null?run.note:"Обновление не завершилось. Ограниченные повторы и история сохранены.");
    });}
    public Map<String,Object> configure(Long id,long storeVersion,long version,String mode,boolean useAi,boolean engine,boolean writes,boolean low,int maximum,int cooldown,List<String> categories,int interval){
        if(!workerAvailable)throw new IllegalStateException("Фоновая служба не включена оператором. Настройки и цены не изменены");
        return tx.execute(t->{prices.configureAutonomy(id,storeVersion,version,mode,useAi,engine,writes,low,maximum,cooldown,categories,interval);queue.retryCycle(id);return detailWithin(id);});
    }
    public Map<String,Object> pause(Long id,long version){return tx.execute(t->{store(id);var p=policy(id);if(p==null||p.version!=version)throw new IllegalStateException("Настройки изменились. Обновите страницу");p.autonomyEnabled=false;p.consentRevision++;queue.cancelCycle(id);audit.record("STORE_AUTONOMY_PAUSED","store:"+id,"Будущие автоматические проверки и изменения остановлены");em.flush();return detailWithin(id);});}
    public Map<String,Object> resume(Long id,long version){return tx.execute(t->{var s=store(id);var p=policy(id);if(!workerAvailable||p==null||p.version!=version||s.archived||s.status.equals("DISCONNECTED"))throw new IllegalStateException("Проверьте службу, подключение и актуальные настройки");if(p.mode.equals("AUTOPILOT")&&(!p.priceWriteConsent||!s.writeEnabled||!p.scheduledAiConsent&&!p.engineFallbackConsent))throw new IllegalStateException("После остановки заново подтвердите границы автоматизации");p.autonomyEnabled=true;p.consentRevision++;queue.retryCycle(id);audit.record("STORE_AUTONOMY_RESUMED","store:"+id,"Проверки возобновлены в сохранённых границах владельца");em.flush();return detailWithin(id);});}
    public Map<String,Object> detail(Long id){return tx.execute(t->{store(id);return detailWithin(id);});}
    private Map<String,Object> detailWithin(Long id){
        var result=new LinkedHashMap<String,Object>();result.put("storeId",id);result.put("policy",prices.settings(id));result.put("workerAvailable",workerAvailable);result.put("task",tasks.findByStoreIdAndOperation(id,"AUTONOMY").orElse(null));result.put("runs",em.createQuery("select r from StoreSyncRun r where r.storeId=:id and r.operation='AUTONOMY' order by r.id desc",StoreSyncRun.class).setParameter("id",id).setMaxResults(15).getResultList());return result;
    }
    public Map<String,Object> dashboard(){return tx.execute(t->{
        var rows=new ArrayList<Map<String,Object>>();LocalDateTime last=null,next=null;int running=0,enabled=0,failed=0;
        for(var s:em.createQuery("select s from ConnectedStore s where s.archived=false and s.platform='WOOCOMMERCE' and s.status<>'DISCONNECTED' order by s.id",ConnectedStore.class).setMaxResults(10).getResultList()){
            var p=policy(s.id);var task=tasks.findByStoreIdAndOperation(s.id,"AUTONOMY").orElse(null);var latest=em.createQuery("select r from StoreSyncRun r where r.storeId=:id and r.operation='AUTONOMY' order by r.id desc",StoreSyncRun.class).setParameter("id",s.id).setMaxResults(1).getResultStream().findFirst().orElse(null);
            var row=new LinkedHashMap<String,Object>();row.put("id",s.id);row.put("name",s.name);row.put("testConnection",s.testConnection);row.put("enabled",p!=null&&p.autonomyEnabled);row.put("mode",p==null?"RECOMMEND":p.mode);row.put("version",p==null?0:p.version);row.put("lastCheck",latest==null?null:latest.completedAt);row.put("taskState",task==null?"NOT_STARTED":task.state);row.put("error",task==null?null:task.lastError);row.put("nextCheck",p!=null&&p.autonomyEnabled&&task!=null&&Set.of("QUEUED","RETRY").contains(task.state)?task.dueAt:null);rows.add(row);
            if(latest!=null&&(last==null||latest.completedAt.isAfter(last)))last=latest.completedAt;
            if(p!=null&&p.autonomyEnabled){enabled++;if(task!=null){if(task.state.equals("RUNNING"))running++;if(task.state.equals("FAILED"))failed++;if(Set.of("QUEUED","RETRY").contains(task.state)&&(next==null||task.dueAt.isBefore(next)))next=task.dueAt;}}
        }
        long applied=em.createQuery("select count(c) from StorePriceChange c where c.automatic=true and c.status='APPLIED' and c.completedAt>=:today",Long.class).setParameter("today",LocalDate.now().atStartOfDay()).getSingleResult();
        long attention=ConnectedPricingService.attentionCount(em)+failed;
        var result=new LinkedHashMap<String,Object>();result.put("workerAvailable",workerAvailable);result.put("enabledStores",enabled);result.put("running",running);result.put("failed",failed);result.put("lastCheck",last);result.put("nextCheck",workerAvailable?next:null);result.put("actionsToday",applied);result.put("analysesToday",em.createQuery("select coalesce(sum(r.imported),0) from StoreSyncRun r where r.operation='AUTONOMY' and r.completedAt>=:today",Long.class).setParameter("today",LocalDate.now().atStartOfDay()).getSingleResult());result.put("attention",attention);result.put("stores",rows);
        result.put("latestChanges",em.createQuery("select c from StorePriceChange c where c.status='APPLIED' and (c.automatic=true or c.revertsId is not null) and c.storeId in(select s.id from ConnectedStore s where s.archived=false) order by c.id desc",StorePriceChange.class).setMaxResults(6).getResultList().stream().map(c->{var v=new LinkedHashMap<String,Object>();v.put("id",c.id);v.put("storeId",c.storeId);v.put("name",c.productName);v.put("source",c.source);v.put("before",c.expectedPrice);v.put("after",c.targetPrice);v.put("currency",c.currency);v.put("time",c.completedAt);v.put("revertsId",c.revertsId);v.put("reverted",em.createQuery("select count(r) from StorePriceChange r where r.revertsId=:id and r.status='APPLIED'",Long.class).setParameter("id",c.id).getSingleResult()>0);return v;}).toList());return result;
    });}
}
