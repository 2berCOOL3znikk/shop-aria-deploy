package com.aria.service;
import com.aria.entity.*;
import com.aria.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
@Service @Transactional
public class StoreSyncQueue {
 private final StoreSyncTaskRepository tasks;private final ConnectedStoreRepository stores;private final AuditService audit;
 public StoreSyncQueue(StoreSyncTaskRepository t,ConnectedStoreRepository s,AuditService a){tasks=t;stores=s;audit=a;}
 public record Claim(Long id,Long storeId,String operation,String lease,long generation,long storeVersion){}
 public StoreSyncTask enqueue(Long id,long version,String operation){
  if(!Set.of("CHECK","SYNC","ORDERS").contains(operation))throw new IllegalArgumentException("Неверный тип обновления");
  var s=stores.lockById(id).orElseThrow(()->new IllegalArgumentException("Магазин не найден"));
  if(s.version!=version)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"Данные изменились. Обновите магазины");
  if(s.status.equals("DISCONNECTED")||(operation.equals("ORDERS")&&!s.platform.equals("WOOCOMMERCE")))throw new IllegalArgumentException("Настройте подключение магазина");
  var task=tasks.findByStoreIdAndOperation(id,operation).orElseGet(StoreSyncTask::new);
  if(task.id!=null)task=tasks.lockById(task.id).orElseThrow();
  var now=LocalDateTime.now();
  if(task.id==null){task.storeId=id;task.operation=operation;task.generation=1;task.state="QUEUED";task.dueAt=now;task.attempts=0;}
  else if(task.state.equals("RUNNING")){task.generation++;}
  else if(!task.state.equals("QUEUED")){task.generation++;task.state="QUEUED";task.attempts=0;task.dueAt=now;task.lease=null;task.leaseUntil=null;task.lastError=null;}
  task.updatedAt=now;tasks.saveAndFlush(task);audit.record("STORE_SYNC_QUEUED","store:"+id,operation+" · задача "+task.id);return task;
 }
 public List<StoreSyncTask> list(Long id){return tasks.findByStoreIdOrderByIdAsc(id);}
 public void requireActive(Claim claim){
  stores.lockById(claim.storeId()).orElseThrow();var task=tasks.lockById(claim.id()).orElseThrow();
  if(!task.state.equals("RUNNING")||!Objects.equals(task.lease,claim.lease())||task.generation!=claim.generation()||task.leaseUntil==null||!task.leaseUntil.isAfter(LocalDateTime.now()))
   throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"Запрос обновления отменён или заменён");
 }
 private boolean due(StoreSyncTask t,LocalDateTime now){return (Set.of("QUEUED","RETRY").contains(t.state)&&!t.dueAt.isAfter(now))||(t.state.equals("RUNNING")&&(t.leaseUntil==null||!t.leaseUntil.isAfter(now)));}
 public Claim claim(){
  var now=LocalDateTime.now();
  for(var candidate:tasks.findAll(org.springframework.data.domain.Sort.by("dueAt","id"))){
   if(!due(candidate,now))continue;
   var store=stores.lockById(candidate.storeId).orElseThrow();var t=tasks.lockById(candidate.id).orElseThrow();
   if(!due(t,now))return null;
   if(store.status.equals("DISCONNECTED")){cancel(t);return null;}
   if(t.state.equals("RUNNING")&&t.generation!=t.claimedGeneration)t.attempts=0;
   if(t.attempts>=3){t.state="FAILED";t.lastError="Обновление прервано трижды. Запустите его повторно";t.lease=null;t.leaseUntil=null;return null;}
   t.attempts++;t.claimedGeneration=t.generation;t.state="RUNNING";t.lease=UUID.randomUUID().toString();t.leaseUntil=now.plusMinutes(5);t.updatedAt=now;
   return new Claim(t.id,t.storeId,t.operation,t.lease,t.generation,store.version);
  }
  return null;
 }
 public void finish(Claim claim,String error){
  stores.lockById(claim.storeId()).orElseThrow();var t=tasks.lockById(claim.id()).orElseThrow();
  if(!t.state.equals("RUNNING")||!Objects.equals(t.lease,claim.lease())||t.leaseUntil==null||!t.leaseUntil.isAfter(LocalDateTime.now()))return;
  var now=LocalDateTime.now();t.lease=null;t.leaseUntil=null;t.updatedAt=now;
  if(t.generation!=claim.generation()){t.state="QUEUED";t.attempts=0;t.dueAt=now;t.lastError=null;return;}
  t.lastError=error;
  if(error==null)t.state="DONE";
  else if(t.attempts>=3)t.state="FAILED";
  else{t.state="RETRY";t.dueAt=now.plusMinutes(t.attempts*2L);}
 }
 public void cancelAll(Long id){stores.lockById(id).orElseThrow();for(var row:tasks.findByStoreIdOrderByIdAsc(id)){var task=tasks.lockById(row.id).orElseThrow();if(Set.of("QUEUED","RUNNING","RETRY").contains(task.state))cancel(task);}}
 private void cancel(StoreSyncTask t){t.state="CANCELLED";t.lease=null;t.leaseUntil=null;t.updatedAt=LocalDateTime.now();}
 public LocalDateTime nextDue(){return tasks.findAll().stream().filter(t->Set.of("QUEUED","RUNNING","RETRY").contains(t.state)).map(t->t.state.equals("RUNNING")?(t.leaseUntil==null?LocalDateTime.now():t.leaseUntil):t.dueAt).min(LocalDateTime::compareTo).orElse(null);}
}
