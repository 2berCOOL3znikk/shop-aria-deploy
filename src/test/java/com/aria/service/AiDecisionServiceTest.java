package com.aria.service;
import com.aria.repository.PricingDecisionRepository;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class AiDecisionServiceTest {
    @Test void managerAiSocialChoiceUsesOwnerPolicyFenceAndNeverBypassesIt(){
        var smart=mock(SmartPricingService.class);var ai=mock(IntelligenceService.class);var social=mock(SocialProposalService.class);var claim=new SmartPricingService.AiClaim(1L,"claim",1L,Map.of());
        when(smart.claimAi(1L,false)).thenReturn(claim);when(ai.decidePricing(anyMap())).thenReturn(Map.of("mode","AI"));when(smart.completeAiFromManager(eq("lease"),eq(claim),anyMap(),eq(false))).thenReturn(new java.util.HashMap<>(Map.of("source","AI","strategy","SOCIAL_REVIEW")));when(social.proposeForManager(1L,"lease")).thenThrow(new IllegalStateException("Owner disabled social drafts"));
        var result=new AiDecisionService(smart,ai,mock(PricingDecisionRepository.class),social).decideForManager(1L,false,"lease");assertThat(result).containsKey("socialProposalNote").doesNotContainKey("socialProposal");verify(social).proposeForManager(1L,"lease");verify(social,never()).propose(anyLong());
    }
    @Test void invalidAiUsesSameClaimForEngineFallbackWithoutAnotherNetworkRequest(){
        var smart=mock(SmartPricingService.class);var ai=mock(IntelligenceService.class);var claim=new SmartPricingService.AiClaim(1L,"claim",1L,Map.of());
        when(smart.claimAi(1L,false)).thenReturn(claim);when(ai.decidePricing(claim.facts())).thenReturn(Map.of("mode","AI"));
        when(smart.completeAi(eq(claim),anyMap(),eq(false))).thenThrow(new IllegalStateException("invalid strategy")).thenReturn(new java.util.HashMap<>(Map.of("source","ENGINE")));
        var service=new AiDecisionService(smart,ai,mock(PricingDecisionRepository.class),mock(SocialProposalService.class));
        assertThat(service.decide(1L,false)).containsEntry("source","ENGINE");verify(ai,times(1)).decidePricing(anyMap());verify(smart,times(2)).completeAi(eq(claim),anyMap(),eq(false));
    }
    @Test void staleConsentCannotBeHiddenByFallback(){
        var smart=mock(SmartPricingService.class);var ai=mock(IntelligenceService.class);var claim=new SmartPricingService.AiClaim(1L,"claim",1L,Map.of());
        when(smart.claimAi(1L,false)).thenReturn(claim);when(ai.decidePricing(claim.facts())).thenReturn(Map.of("mode","AI"));
        when(smart.completeAi(eq(claim),anyMap(),eq(false))).thenThrow(new IllegalStateException("consent changed"));
        var service=new AiDecisionService(smart,ai,mock(PricingDecisionRepository.class),mock(SocialProposalService.class));assertThatThrownBy(()->service.decide(1L,false)).isInstanceOf(IllegalStateException.class);
    }
}
