package com.aria.controller;
import com.aria.service.StorePriceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
@RestController @RequestMapping("/api/admin/control/stores/{storeId}/pricing")
public class StorePriceController {
 private final StorePriceService service;public StorePriceController(StorePriceService s){service=s;}
 public record Settings(@Min(0) long storeVersion,@Min(0) long version,@NotNull @Pattern(regexp="RECOMMEND|AUTOPILOT") String mode,boolean externalAiConsent,boolean scheduledAiConsent,boolean priceWriteConsent,boolean allowLowConfidence,@Min(0) @Max(50) int maximumDiscount,@Min(1) @Max(720) int cooldownHours,@NotNull @Size(max=30) List<@NotBlank @Size(max=200) String> allowedCategories,@AssertTrue boolean confirmed){}
 public record Facts(@Min(0) long version,@NotNull @DecimalMin("0") @Digits(integer=16,fraction=2) BigDecimal unitCost,@NotNull @DecimalMin("0.01") @Digits(integer=16,fraction=2) BigDecimal minimumPrice,@NotBlank @Size(max=200) String category,boolean nonPerishableConfirmed,@NotBlank @Size(max=900) String reason,@AssertTrue boolean confirmed){}
 public record Change(@Min(0) long storeVersion,@NotBlank @Pattern(regexp="[1-9][0-9]{0,18}") String externalId,@NotNull @DecimalMin("0.01") @Digits(integer=16,fraction=2) BigDecimal expectedPrice,@NotNull @DecimalMin("0.01") @Digits(integer=16,fraction=2) BigDecimal targetPrice,@NotNull @Pattern(regexp="[A-Za-z0-9_-]{10,80}") String requestKey,@NotBlank @Size(max=900) String reason,@AssertTrue boolean confirmed){}
 @GetMapping("/settings") public Object settings(@PathVariable Long storeId){return service.settings(storeId);}
 @PutMapping("/settings") public Object configure(@PathVariable Long storeId,@Valid @RequestBody Settings s){return service.configure(storeId,s.storeVersion(),s.version(),s.mode(),s.externalAiConsent(),s.scheduledAiConsent(),s.priceWriteConsent(),s.allowLowConfidence(),s.maximumDiscount(),s.cooldownHours(),s.allowedCategories());}
 @PostMapping("/stop") public Object stop(@PathVariable Long storeId){return service.stop(storeId);}
 @GetMapping("/facts") public Object facts(@PathVariable Long storeId){return service.productFacts(storeId);}
 @PutMapping("/facts/{externalId}") public Object facts(@PathVariable Long storeId,@PathVariable String externalId,@Valid @RequestBody Facts f){return service.confirmProduct(storeId,externalId,f.version(),f.unitCost(),f.minimumPrice(),f.category(),f.nonPerishableConfirmed(),f.reason());}
 @GetMapping("/changes") public Object history(@PathVariable Long storeId){return service.history(storeId);}
 @PostMapping("/changes") public Object change(@PathVariable Long storeId,@Valid @RequestBody Change c){return service.request(storeId,c.storeVersion(),c.externalId(),c.expectedPrice(),c.targetPrice(),c.requestKey(),c.reason());}
 @PostMapping("/changes/{id}/reconcile") public Object reconcile(@PathVariable Long storeId,@PathVariable Long id){return service.reconcile(storeId,id);}
}
