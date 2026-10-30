package com.aria.service;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import java.util.*;
import java.util.concurrent.*;
import java.time.*;
@Service
public class StoreSyncWorker {
 private final StoreSyncQueue queue;private final ConnectedStoresService stores;private final boolean enabled;
 private final ScheduledExecutorService executor=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"aria-store-sync");t.setDaemon(true);return t;});
 private ScheduledFuture<?> next;private long nextAt=Long.MAX_VALUE;
 public StoreSyncWorker(StoreSyncQueue q,ConnectedStoresService s,@Value("${aria.store-sync.worker-enabled:true}") boolean enabled){queue=q;stores=s;this.enabled=enabled;}
 @EventListener(ApplicationReadyEvent.class) public void startup(){wake();}
 public void wake(){schedule(0);}
 private synchronized void schedule(long delay){if(!enabled||executor.isShutdown())return;long at=System.currentTimeMillis()+Math.max(0,delay);if(next!=null&&!next.isDone()&&at>=nextAt)return;if(next!=null)next.cancel(false);nextAt=at;next=executor.schedule(this::drain,Math.max(0,delay),TimeUnit.MILLISECONDS);}
 private void drain(){synchronized(this){next=null;nextAt=Long.MAX_VALUE;}try{for(int n=0;n<30&&processOne();n++){} }catch(RuntimeException e){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Store sync interrupted ({})",e.getClass().getSimpleName());}finally{try{var due=queue.nextDue();if(due!=null)schedule(Math.max(1000,Duration.between(LocalDateTime.now(),due).toMillis()));}catch(RuntimeException e){schedule(60000);}}}
 public boolean processOne(){
  var claim=queue.claim();if(claim==null)return false;String error=null;
  try{Object result=stores.executeQueued(claim);var map=(Map<?,?>)result;Object failure=map.get(claim.operation().equals("ORDERS")?"lastOrdersError":"lastError");if(failure!=null)error=failure.toString();}
  catch(org.springframework.web.server.ResponseStatusException e){error="Настройки магазина изменились. Обновление будет проверено заново";}
  catch(RuntimeException e){error="Обновление не завершилось. Предыдущие данные сохранены";}
  queue.finish(claim,error);return true;
 }
 @jakarta.annotation.PreDestroy public void stop(){executor.shutdownNow();}
}
