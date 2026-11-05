package com.aria.config;
import com.aria.repository.ConnectedStoreRepository;
import com.aria.service.ConnectedPricingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
/** One bounded inference per minute; the same company-wide consent/budget is used. */
@Component
public class ConnectedPricingSchedule {
 private final ConnectedStoreRepository stores;private final ConnectedPricingService pricing;private final boolean enabled;
 public ConnectedPricingSchedule(ConnectedStoreRepository stores,ConnectedPricingService pricing,@Value("${aria.connected-pricing.schedule-enabled:true}") boolean enabled){this.stores=stores;this.pricing=pricing;this.enabled=enabled;}
 @Scheduled(fixedDelay=60000,initialDelay=90000) public void next(){if(!enabled)return;for(var s:stores.findAll())if(!s.archived&&s.status.equals("CONNECTED")&&s.platform.equals("WOOCOMMERCE"))try{if(pricing.backgroundOne(s.id))return;}catch(RuntimeException ignored){org.slf4j.LoggerFactory.getLogger(getClass()).info("Connected catalogue analysis deferred");}}
}
