package com.aria.controller;
import com.aria.service.ConnectedPricingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/control/stores/{storeId}/pricing/decisions")
public class ConnectedPricingController {
 private final ConnectedPricingService service;public ConnectedPricingController(ConnectedPricingService service){this.service=service;}
 public record Confirmation(@AssertTrue boolean confirmed){}
 public record Rejection(@NotBlank @Size(max=900) String reason){}
 @GetMapping public Object list(@PathVariable Long storeId){return service.list(storeId);}
 @PostMapping("/analyze") public Object analyze(@PathVariable Long storeId){return service.analyze(storeId);}
 @PostMapping("/{id}/ai") public Object ai(@PathVariable Long storeId,@PathVariable Long id){return service.decide(storeId,id,false);}
 @PostMapping("/{id}/apply") public Object apply(@PathVariable Long storeId,@PathVariable Long id,@Valid @RequestBody Confirmation c){return service.apply(storeId,id,false);}
 @PostMapping("/{id}/reject") public Object reject(@PathVariable Long storeId,@PathVariable Long id,@Valid @RequestBody Rejection r){service.reject(storeId,id,r.reason());return java.util.Map.of("rejected",true);}
}
