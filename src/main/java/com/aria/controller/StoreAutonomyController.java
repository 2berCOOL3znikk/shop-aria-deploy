package com.aria.controller;

import com.aria.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/admin/control/stores/{storeId}/autonomy")
public class StoreAutonomyController {
 private final StoreAutonomyService service;private final StoreSyncWorker worker;
 public StoreAutonomyController(StoreAutonomyService service,StoreSyncWorker worker){this.service=service;this.worker=worker;}
 public record Setup(@Min(0) long storeVersion,@Min(0) long version,@NotNull @Pattern(regexp="RECOMMEND|AUTOPILOT") String mode,boolean useAi,boolean engineFallbackConsent,boolean priceWriteConsent,boolean allowLowConfidence,@Min(0) @Max(50) int maximumDiscount,@Min(1) @Max(720) int cooldownHours,@NotNull @Size(max=30) List<@NotBlank @Size(max=200) String> allowedCategories,@Min(15) @Max(1440) int intervalMinutes,@AssertTrue boolean confirmed){}
 public record Version(@Min(0) long version,@AssertTrue boolean confirmed){}
 @GetMapping public Object detail(@PathVariable Long storeId){return service.detail(storeId);}
 @PutMapping public Object configure(@PathVariable Long storeId,@Valid @RequestBody Setup s){var result=service.configure(storeId,s.storeVersion(),s.version(),s.mode(),s.useAi(),s.engineFallbackConsent(),s.priceWriteConsent(),s.allowLowConfidence(),s.maximumDiscount(),s.cooldownHours(),s.allowedCategories(),s.intervalMinutes());worker.wake();return result;}
 @PostMapping("/pause") public Object pause(@PathVariable Long storeId,@Valid @RequestBody Version v){return service.pause(storeId,v.version());}
 @PostMapping("/resume") public Object resume(@PathVariable Long storeId,@Valid @RequestBody Version v){var result=service.resume(storeId,v.version());worker.wake();return result;}
}
