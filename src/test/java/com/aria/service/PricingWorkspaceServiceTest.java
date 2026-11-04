package com.aria.service;

import com.aria.entity.PricingDecision;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Map;
import static com.aria.service.PricingWorkspaceService.View.*;
import static org.assertj.core.api.Assertions.*;

class PricingWorkspaceServiceTest {
    private PricingDecision decision(){var d=new PricingDecision();d.status="PENDING";d.source="ENGINE";d.strategy="PRICE";return d;}
    private Map<String,Object> report(String action,int current,int proposed){return Map.of("action",action,"facts",Map.of("currentPercent",current),"suggestedPercent",proposed);}
    @Test void keepIsNotPretendedToBeAnAttentionOrModelError(){assertThat(PricingWorkspaceService.categories(decision(),report("KEEP",0,0),false)).containsExactlyInAnyOrder(ALL,UNCHANGED);}
    @Test void changedPriceAwaitsConfirmation(){assertThat(PricingWorkspaceService.categories(decision(),report("DISCOUNT",0,10),false)).contains(ALL,ATTENTION,PENDING).doesNotContain(UNCHANGED,ERROR);}
    @Test void modelKeepOverridesAnEngineDiscount(){var d=decision();d.source="AI";d.strategy="KEEP";d.selectedPercent=0;assertThat(PricingWorkspaceService.categories(d,report("DISCOUNT",0,20),false)).contains(UNCHANGED).doesNotContain(PENDING);}
    @Test void blockedAndReviewCannotBeApprovedAsPriceChanges(){for(String action:new String[]{"BLOCKED","REVIEW"})assertThat(PricingWorkspaceService.categories(decision(),report(action,0,0),false)).contains(ATTENTION).doesNotContain(PENDING,UNCHANGED);}
    @Test void automaticClassificationNeedsTheActualAppliedAuditRecord(){var d=decision();d.status="APPLIED";assertThat(PricingWorkspaceService.categories(d,report("DISCOUNT",0,10),false)).doesNotContain(AUTOMATIC);assertThat(PricingWorkspaceService.categories(d,report("DISCOUNT",0,10),true)).contains(AUTOMATIC);d.status="ROLLED_BACK";assertThat(PricingWorkspaceService.categories(d,report("DISCOUNT",0,10),true)).doesNotContain(AUTOMATIC);}
    @Test void missingConsentOrSkippedAttemptDoesNotPretendTheProviderFailed(){var d=decision();d.aiLastSkippedAt=LocalDateTime.now();d.aiLastSkippedNote="Согласие выключено";assertThat(PricingWorkspaceService.categories(d,report("KEEP",0,0),false)).contains(ATTENTION).doesNotContain(ERROR);d.aiCompletedAt=LocalDateTime.now();assertThat(PricingWorkspaceService.categories(d,report("KEEP",0,0),false)).contains(ERROR);}
    @Test void corruptReportRemainsVisibleAsAnError(){assertThat(PricingWorkspaceService.categories(decision(),Map.of(),false)).contains(ALL,ATTENTION,ERROR).doesNotContain(PENDING);}
    @Test void socialOrPurchaseStrategiesAreNotPriceApprovalButtons(){var d=decision();d.source="AI";d.strategy="SOCIAL_REVIEW";d.selectedPercent=0;assertThat(PricingWorkspaceService.categories(d,report("KEEP",0,0),false)).contains(ATTENTION).doesNotContain(PENDING,UNCHANGED);}
}
