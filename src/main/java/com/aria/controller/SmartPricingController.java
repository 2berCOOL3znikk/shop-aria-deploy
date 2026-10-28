package com.aria.controller;
import com.aria.service.*;
import com.aria.dto.ControlRequests.ReasonInput;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/admin/control/smart-pricing")
public class SmartPricingController {
    private final SmartPricingService smart;private final IntelligenceService intelligence;
    public SmartPricingController(SmartPricingService smart,IntelligenceService intelligence){this.smart=smart;this.intelligence=intelligence;}
    public record Approval(@Min(0) @Max(90) Integer percent,@NotBlank @Size(max=900) String reason){}
    public record SettingsInput(Long version,@NotBlank @Pattern(regexp="RECOMMEND|AUTOPILOT") String mode,
        @Min(0) @Max(90) int maximumDiscount,@Min(1) @Max(720) int cooldownHours,
        boolean allowLowConfidence,boolean allowRestore,@NotNull @Size(max=50) List<@NotBlank @Size(max=120) String> allowedCategories,
        Boolean externalAiEnabled,Boolean scheduledAiEnabled,@Min(0) @Max(50) Integer aiDailyBudget){}
    @GetMapping("/settings") public Map<String,Object> settings(){return smart.settings();}
    @PutMapping("/settings") public Map<String,Object> configure(@Valid @RequestBody SettingsInput input){return smart.configure(input);}
    @PostMapping("/stop") public Map<String,Object> stop(){return smart.stop();}
    @GetMapping public Map<String,Object> list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size){return smart.list(page,size);}
    @PostMapping("/analyze") public Map<String,Object> analyze(@RequestParam(defaultValue="false") boolean onlyDue){return smart.analyzeAll(onlyDue);}
    @PostMapping("/products/{id}/analyze") public Map<String,Object> product(@PathVariable Long id){return smart.analyze(id);}
    @PostMapping("/{id}/apply") public Map<String,Object> apply(@PathVariable Long id,@Valid @RequestBody Approval approval){return smart.apply(id,approval.percent(),approval.reason());}
    @PostMapping("/{id}/reject") public Map<String,Object> reject(@PathVariable Long id,@Valid @RequestBody ReasonInput input){return smart.reject(id,input.reason());}
    @PostMapping("/{id}/rollback") public Map<String,Object> rollback(@PathVariable Long id,@Valid @RequestBody ReasonInput input){return smart.rollback(id,input.reason());}
    @GetMapping("/products/{id}/history") public List<Map<String,Object>> history(@PathVariable Long id){return smart.history(id);}
    @PostMapping("/{id}/explain") public Map<String,Object> explain(@PathVariable Long id){return smart.saveExplanation(id,intelligence.explainPricing(smart.explanationFacts(id)));}
}
