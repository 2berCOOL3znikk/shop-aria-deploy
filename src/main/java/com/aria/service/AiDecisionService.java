package com.aria.service;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import com.aria.repository.PricingDecisionRepository;
import org.springframework.data.domain.PageRequest;
import java.time.LocalDateTime;
import java.util.Map;

/** Remote inference is outside all database transactions and inventory locks. */
@Service
public class AiDecisionService {
    private final SmartPricingService smart;private final IntelligenceService ai;private final PricingDecisionRepository decisions;
    @Value("${aria.smart-pricing.schedule-enabled:true}") private boolean scheduling;
    public AiDecisionService(SmartPricingService smart,IntelligenceService ai,PricingDecisionRepository decisions){this.smart=smart;this.ai=ai;this.decisions=decisions;}
    public Map<String,Object> decide(Long id,boolean scheduled){
        var claim=smart.claimAi(id,scheduled);var result=ai.decidePricing(claim.facts());return smart.completeAi(claim,result,scheduled);
    }
    public Map<String,Object> status(){return ai.pricingStatus();}
    @Scheduled(fixedDelay=60000,initialDelay=60000)
    public void nextScheduled(){
        if(!scheduling)return;var config=smart.settings();
        if(!Boolean.TRUE.equals(config.get("externalAiEnabled"))||!Boolean.TRUE.equals(config.get("scheduledAiEnabled")))return;
        var now=LocalDateTime.now();var ids=decisions.aiCandidates(now,now.minusHours(1),now.minusSeconds(90),PageRequest.of(0,1));
        if(ids.isEmpty())return;
        try{decide(ids.getFirst(),true);}catch(RuntimeException e){org.slf4j.LoggerFactory.getLogger(AiDecisionService.class).info("Scheduled AI skipped ({})",e.getClass().getSimpleName());}
    }
}
