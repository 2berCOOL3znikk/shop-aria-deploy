package com.aria.controller;
import com.aria.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;
@RestController @RequestMapping("/api/admin/control/social-proposals")
public class SocialProposalController {
    private final SocialProposalService proposals;private final CharityEligibilityService charities;
    public SocialProposalController(SocialProposalService proposals,CharityEligibilityService charities){this.proposals=proposals;this.charities=charities;}
    public record Acceptance(@NotBlank @Size(max=120) String region,@NotEmpty @Size(max=30) List<@NotBlank @Size(max=120) String> categories,@NotBlank @Size(max=1000) String storageConditions,@Min(0) @Max(365) int minimumRemainingDays,boolean active,@AssertTrue boolean informationConfirmed){}
    public record Approval(@NotNull Long organizationId,@Min(1) @Max(1000000) int quantity,@NotNull LocalDate validUntil,@NotBlank @Size(max=900) String reason,@AssertTrue boolean suitabilityConfirmed,@AssertTrue boolean transportConfirmed){}
    @GetMapping public List<Map<String,Object>> list(){return proposals.list();}
    @GetMapping("/organizations") public List<Map<String,Object>> organizations(){return charities.organizations();}
    @PutMapping("/organizations/{id}") public Map<String,String> acceptance(@PathVariable Long id,@Valid @RequestBody Acceptance in){charities.save(id,in.region(),in.categories(),in.storageConditions(),in.minimumRemainingDays(),in.active());return Map.of("message","Условия сохранены");}
    @PostMapping("/decisions/{id}") public Map<String,Object> propose(@PathVariable Long id){return proposals.propose(id);}
    @PostMapping("/{id}/approve") public Map<String,Object> approve(@PathVariable Long id,@Valid @RequestBody Approval in){return proposals.approve(id,in.organizationId(),in.quantity(),in.validUntil(),in.reason());}
    @PostMapping("/{id}/cancel") public Map<String,Object> cancel(@PathVariable Long id,@Valid @RequestBody com.aria.dto.ControlRequests.ReasonInput in){return proposals.cancel(id,in.reason());}
}
