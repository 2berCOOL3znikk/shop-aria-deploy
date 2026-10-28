package com.aria.controller;
import com.aria.service.AiDecisionService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/admin/control/smart-pricing")
public class AiDecisionController {
    private final AiDecisionService ai;
    public AiDecisionController(AiDecisionService ai){this.ai=ai;}
    @GetMapping("/ai-status") public Map<String,Object> status(){return ai.status();}
    @PostMapping("/{id}/ai-decide") public Map<String,Object> decide(@PathVariable Long id){return ai.decide(id,false);}
}
