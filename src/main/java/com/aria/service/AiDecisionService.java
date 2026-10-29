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
    private final SocialProposalService social;
    @Value("${aria.smart-pricing.schedule-enabled:true}") private boolean scheduling;
    public AiDecisionService(SmartPricingService smart,IntelligenceService ai,PricingDecisionRepository decisions,SocialProposalService social){this.smart=smart;this.ai=ai;this.decisions=decisions;this.social=social;}
    public Map<String,Object> decide(Long id,boolean scheduled){
        return decideInternal(id,scheduled,true,null);
    }
    public Map<String,Object> decideForManager(Long id,boolean autopilot,String lease){return decideInternal(id,false,autopilot,lease);}
    private Map<String,Object> decideInternal(Long id,boolean scheduled,boolean allowAutopilot,String lease){
        var claim=smart.claimAi(id,scheduled);var result=ai.decidePricing(claim.facts());Map<String,Object> saved;
        try{saved=lease!=null?smart.completeAiFromManager(lease,claim,result,allowAutopilot&&"AI".equals(result.get("mode"))):smart.completeAi(claim,result,scheduled);}catch(IllegalStateException invalid){
            if(!"AI".equals(result.get("mode")))throw invalid;
            // Rechecks the same claim and consent: stale data or stop still fails, invalid AI strategy falls back.
            var fallback=Map.<String,Object>of("mode","ENGINE","note","AI-стратегия не прошла проверку. Сохранён расчёт Java.");
            saved=lease!=null?smart.completeAiFromManager(lease,claim,fallback,false):smart.completeAi(claim,fallback,scheduled);
        }
        if("AI".equals(saved.get("source"))&&"SOCIAL_REVIEW".equals(saved.get("strategy"))){try{saved.put("socialProposal",social.propose(id));}catch(IllegalStateException e){saved.put("socialProposalNote","Черновик не создан: пересчитайте данные и проверьте пригодность");}}
        return saved;
    }
    public Map<String,Object> status(){var status=new java.util.HashMap<String,Object>(ai.pricingStatus());status.put("scheduledAvailable",scheduling);return status;}
    @Scheduled(fixedDelay=60000,initialDelay=60000)
    public void nextScheduled(){
        if(!scheduling)return;var config=smart.settings();
        if(!Boolean.TRUE.equals(config.get("externalAiEnabled"))||!Boolean.TRUE.equals(config.get("scheduledAiEnabled")))return;
        if(((Number)config.get("aiBudgetUsed")).intValue()>=((Number)config.get("aiDailyBudget")).intValue())return;
        var now=LocalDateTime.now();var ids=decisions.aiCandidates(now,now.minusHours(1),now.minusSeconds(90),PageRequest.of(0,1));
        if(ids.isEmpty())return;
        try{decide(ids.getFirst(),true);}catch(RuntimeException e){
            smart.recordSkippedAi(ids.getFirst(),e instanceof IllegalStateException?e.getMessage():"Временная ошибка доступа к данным");
            org.slf4j.LoggerFactory.getLogger(AiDecisionService.class).info("Scheduled AI skipped ({})",e.getClass().getSimpleName());
        }
    }
}
