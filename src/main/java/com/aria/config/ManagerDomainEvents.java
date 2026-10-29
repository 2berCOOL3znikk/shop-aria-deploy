package com.aria.config;
import com.aria.service.*;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.event.*;
@Component
public class ManagerDomainEvents {
 private final ManagerQueue queue;private final AiManagerService manager;
 @Value("${aria.manager.enabled:true}") private boolean enabled;
 public ManagerDomainEvents(ManagerQueue q,AiManagerService m){queue=q;manager=m;}
 @TransactionalEventListener(phase=TransactionPhase.BEFORE_COMMIT)
 public void persist(StoreChanged event){if(enabled)queue.ensure(event.productId(),event.cause(),true);}
 @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
 public void wake(StoreChanged event){if(!enabled)return;try{manager.activity();}catch(RuntimeException e){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Committed task will resume after catch-up ({})",e.getClass().getSimpleName());}}
}
