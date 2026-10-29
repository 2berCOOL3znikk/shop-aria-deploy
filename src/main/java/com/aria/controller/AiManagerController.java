package com.aria.controller;
import com.aria.service.*;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import java.math.BigDecimal;
@RestController @RequestMapping("/api/admin/control/manager")
public class AiManagerController {
 private final AiManagerService manager;private final ManagerStore store;private final DemandAnalyticsService analytics;
 public AiManagerController(AiManagerService m,ManagerStore s,DemandAnalyticsService a){manager=m;store=s;analytics=a;}
 public record Settings(@Min(0) long revision,@NotBlank @Pattern(regexp="OFF|ADVISOR|AUTOPILOT") String mode,boolean automaticAiConsent,boolean socialDrafts,boolean purchaseDrafts,@Min(5) @Max(1440) int intervalMinutes,@NotNull @DecimalMin("0.00") @DecimalMax("9999999999.99") @Digits(integer=10,fraction=2) BigDecimal dailyDraftBudget,Long pricingVersion,boolean policyConfirmed){}
 @GetMapping public Map<String,Object> status(){return manager.status();}
 @PutMapping("/settings") public Map<String,Object> configure(@Valid @RequestBody Settings s){store.configure(s.revision(),s.mode(),s.automaticAiConsent(),s.socialDrafts(),s.purchaseDrafts(),s.intervalMinutes(),s.dailyDraftBudget(),s.pricingVersion(),s.policyConfirmed());return manager.status();}
 @PostMapping("/run") public Map<String,Object> run(){return Map.of("accepted",manager.enqueue(true),"note","Цикл поставлен в очередь. Состояние сохраняется в журнале; повторный запуск ограничен.");}
 @PostMapping("/stop") public Map<String,Object> stop(){manager.stop();return manager.status();}
 @PostMapping("/actions/{id}/acknowledge") public Map<String,String> acknowledge(@PathVariable Long id){store.acknowledge(id);return Map.of("message","Отмечено как прочитанное");}
 @GetMapping("/analytics/{id}") public Map<String,Object> analytics(@PathVariable Long id){return analytics.product(id);}
}
