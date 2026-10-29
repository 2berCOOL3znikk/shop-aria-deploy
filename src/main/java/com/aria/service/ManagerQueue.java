package com.aria.service;
import com.aria.entity.*;
import com.aria.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.event.*;
import org.springframework.beans.factory.annotation.Value;
import java.time.*;
import java.util.*;

@Service
public class ManagerQueue {
 private final ManagerTaskRepository tasks;private final ManagerExecutionRepository executions;
 private final ProductRepository products;private final AiManagerStateRepository states;private final AuditService audit;
 public ManagerQueue(ManagerTaskRepository t,ManagerExecutionRepository e,ProductRepository p,AiManagerStateRepository s,AuditService a){tasks=t;executions=e;products=p;states=s;audit=a;}
 @Transactional(propagation=Propagation.REQUIRES_NEW)
 public void enqueue(Long product,String cause){ensure(product,cause,true);}
 @Transactional public void ensure(Long product,String cause,boolean changed){
  products.lockById(product).orElseThrow();var task=tasks.lockProduct(product).orElseGet(()->{var t=new ManagerTask();t.productId=product;return t;});
  if(task.id==null||changed){if(task.id!=null)task.generation++;task.cause=cause.length()>300?cause.substring(0,300):cause;task.updatedAt=LocalDateTime.now();task.lastError=null;
   if(!"RUNNING".equals(task.status)){task.status="QUEUED";task.dueAt=LocalDateTime.now();task.attempts=0;}tasks.saveAndFlush(task);}
 }
 public record Claim(Long id,Long productId,String lease,long generation){}
 @Transactional public List<Long> due(){return tasks.due(LocalDateTime.now(),org.springframework.data.domain.PageRequest.of(0,50));}
 @Transactional public Claim claim(Long id,String managerLease){
  var state=states.lockState().orElseThrow();var now=LocalDateTime.now();if("OFF".equals(state.mode)||!Objects.equals(state.lease,managerLease)||state.leaseUntil==null||!state.leaseUntil.isAfter(now))return null;
  var t=tasks.lockTask(id).orElseThrow();if("CANCELLED".equals(t.status)||"FAILED".equals(t.status)||t.dueAt.isAfter(now)||"RUNNING".equals(t.status)&&t.leaseUntil!=null&&t.leaseUntil.isAfter(now))return null;
  if("RUNNING".equals(t.status))executions.findByLease(t.lease).ifPresent(e->{e.status="INTERRUPTED";e.finishedAt=now;e.summary="Аренда истекла; задача возвращена в обработку";});
  if(t.attempts>=3){t.status="FAILED";t.lastError="Три попытки исчерпаны. Нужна проверка причины или новое событие товара.";return null;}
  t.status="RUNNING";t.claimedGeneration=t.generation;t.attempts++;t.lease=UUID.randomUUID().toString();t.leaseUntil=now.plusMinutes(5);t.updatedAt=now;
  var e=new ManagerExecution();e.taskId=t.id;e.productId=t.productId;e.lease=t.lease;executions.save(e);return new Claim(t.id,t.productId,t.lease,t.generation);
 }
 @Transactional public void complete(Claim claim,LocalDateTime next,String source,String strategy,Long decision,boolean price,boolean purchase,boolean social,String summary){
  var t=tasks.lockTask(claim.id()).orElseThrow();if(!Objects.equals(t.lease,claim.lease()))return;
  var now=LocalDateTime.now();t.status="CLOSED".equals(strategy)?"CANCELLED":t.generation==claim.generation()?"WAITING":"QUEUED";t.dueAt="QUEUED".equals(t.status)?now:next;t.attempts=0;t.lease=null;t.leaseUntil=null;t.lastError=null;t.updatedAt=now;
  executions.findByLease(claim.lease()).ifPresent(e->{e.status="DONE";e.finishedAt=now;e.source=source;e.strategy=strategy;e.decisionId=decision;e.priceApplied=price;e.purchaseDraft=purchase;e.socialDraft=social;e.summary=summary.length()>1000?summary.substring(0,1000):summary;});
 }
 @Transactional public void fail(Claim claim,String error){
  var t=tasks.lockTask(claim.id()).orElseThrow();if(!Objects.equals(t.lease,claim.lease()))return;var now=LocalDateTime.now();boolean changed=t.generation!=claim.generation();t.status=changed?"QUEUED":t.attempts>=3?"FAILED":"RETRY";if(changed)t.attempts=0;t.dueAt=now.plusMinutes(changed?0:Math.min(60,1L<<t.attempts));t.lastError=error.length()>700?error.substring(0,700):error;t.lease=null;t.leaseUntil=null;t.updatedAt=now;
  executions.findByLease(claim.lease()).ifPresent(e->{e.status="FAILED";e.finishedAt=now;e.summary=t.lastError;});audit.record("MANAGER_TASK_FAILED","product:"+t.productId,t.lastError);
 }
 @Transactional(readOnly=true) public LocalDateTime nextPending(){return tasks.nextPending();}
 @Transactional public void retry(Long id,String reason){var t=tasks.findById(id).orElseThrow(()->new IllegalArgumentException("Задача не найдена"));if(!"FAILED".equals(t.status))throw new IllegalStateException("Ручной повтор доступен после исчерпания автоматических попыток");ensure(t.productId,"ADMIN_RETRY",true);audit.record("MANAGER_TASK_RETRY","product:"+t.productId,reason);}
 @Transactional(readOnly=true) public Map<String,Object> dashboard(){
  var counts=new LinkedHashMap<String,Object>();for(var r:tasks.counts())counts.put(String.valueOf(r[0]),r[1]);var row=executions.today(LocalDate.now().atStartOfDay()).getFirst();var daily=new LinkedHashMap<String,Object>();String[] keys={"completed","products","priceApplied","purchaseDrafts","socialDrafts","aiDecisionsUsed"};for(int i=0;i<keys.length;i++)daily.put(keys[i],row[i]==null?0:row[i]);var pending=tasks.findTop100ByOrderByUpdatedAtDescIdDesc();var feed=executions.findTop50ByOrderByStartedAtDescIdDesc();var ids=new HashSet<Long>();pending.forEach(t->ids.add(t.productId));feed.forEach(e->ids.add(e.productId));var names=new HashMap<Long,String>();if(!ids.isEmpty())for(var p:products.namesForIds(ids))names.put((Long)p[0],String.valueOf(p[1]));return Map.of("counts",counts,"today",daily,"tasks",pending,"feed",feed,"productNames",names);
 }
}
