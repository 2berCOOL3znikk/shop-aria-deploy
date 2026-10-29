package com.aria.controller;
import com.aria.service.*;
import com.aria.dto.ControlRequests.ReasonInput;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/admin/control/smart-pricing")
public class SmartPricingController {
    private final SmartPricingService smart;private final IntelligenceService intelligence;private final AiManagerService manager;
    public SmartPricingController(SmartPricingService smart,IntelligenceService intelligence,AiManagerService manager){this.smart=smart;this.intelligence=intelligence;this.manager=manager;}
    public record Approval(@Min(0) @Max(90) Integer percent,@NotBlank @Size(max=900) String reason){}
    public record SettingsInput(Long version,@NotBlank @Pattern(regexp="RECOMMEND|AUTOPILOT") String mode,
        @Min(0) @Max(90) int maximumDiscount,@Min(1) @Max(720) int cooldownHours,
        boolean allowLowConfidence,boolean allowRestore,@NotNull @Size(max=50) List<@NotBlank @Size(max=120) String> allowedCategories,
        Boolean externalAiEnabled,Boolean scheduledAiEnabled,@Min(0) @Max(50) Integer aiDailyBudget,
        Boolean publicAssistantAiEnabled,@Min(1) @Max(20) Integer publicAiDailyBudget){}
    @GetMapping("/settings") public Map<String,Object> settings(){return smart.settings();}
    @PutMapping("/settings") public Map<String,Object> configure(@Valid @RequestBody SettingsInput input){return smart.configure(input);}
    @PostMapping("/stop") public Map<String,Object> stop(){manager.stopPricing();return smart.settings();}
    @GetMapping public Map<String,Object> list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size){return smart.list(page,size);}
    @GetMapping("/{id}") public Map<String,Object> detail(@PathVariable Long id){return smart.detail(id);}
    @PostMapping("/analyze") public Map<String,Object> analyze(@RequestParam(defaultValue="false") boolean onlyDue){return smart.analyzeAll(onlyDue);}
    @PostMapping("/products/{id}/analyze") public Map<String,Object> product(@PathVariable Long id){return smart.analyze(id);}
    @PostMapping("/{id}/apply") public Map<String,Object> apply(@PathVariable Long id,@Valid @RequestBody Approval approval){return smart.apply(id,approval.percent(),approval.reason());}
    @PostMapping("/{id}/reject") public Map<String,Object> reject(@PathVariable Long id,@Valid @RequestBody ReasonInput input){return smart.reject(id,input.reason());}
    @PostMapping("/{id}/rollback") public Map<String,Object> rollback(@PathVariable Long id,@Valid @RequestBody ReasonInput input){return smart.rollback(id,input.reason());}
    @GetMapping("/products/{id}/history") public List<Map<String,Object>> history(@PathVariable Long id){return smart.history(id);}
    @PostMapping("/{id}/explain") public Map<String,Object> explain(@PathVariable Long id){
        if(!Boolean.TRUE.equals(smart.settings().get("externalAiEnabled")))return smart.saveExplanation(id,Map.of("mode","ENGINE","note","Сначала разрешите внешний AI в настройках. Расчёт Java доступен без передачи данных."));
        return smart.saveExplanation(id,intelligence.explainPricing(smart.explanationFacts(id)));
    }
}
