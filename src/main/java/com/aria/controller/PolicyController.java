package com.aria.controller;
import com.aria.dto.ControlRequests.PolicyInput;
import com.aria.entity.PricingPolicy;
import com.aria.service.PolicyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/admin/control/policies")
public class PolicyController {
    private final PolicyService policies;
    public PolicyController(PolicyService policies){this.policies=policies;}
    @GetMapping public List<PricingPolicy> list(){return policies.list();}
    @PutMapping public PricingPolicy save(@Valid @RequestBody PolicyInput input){return policies.save(input);}
    @DeleteMapping public java.util.Map<String,String> reset(@RequestParam String category){policies.resetCategory(category);return java.util.Map.of("message","Категория наследует общее правило");}
}
