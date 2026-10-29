package com.aria.controller;
import com.aria.service.PurchaseService;
import com.aria.dto.ControlRequests.ReasonInput;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
@RestController @RequestMapping("/api/admin/control/purchases")
public class PurchaseController {
 private final PurchaseService service;public PurchaseController(PurchaseService s){service=s;}
 public record Quote(@NotNull Long productId,@NotNull Long supplierId,@NotNull @DecimalMin("0.00") @DecimalMax("9999999999.99") @Digits(integer=10,fraction=2) BigDecimal unitCost,@Min(1) @Max(1000000) int minimumQuantity,@NotNull @Min(0) @Max(365) Integer leadDays,@NotNull @PastOrPresent LocalDate quotedOn,@NotNull LocalDate validUntil,boolean informationConfirmed){}
 public record Draft(@NotNull Long quoteId,@Min(1) @Max(1000000) int quantity,@NotBlank @Size(max=900) String reason){}
 public record Receipt(@NotBlank @Pattern(regexp="[A-Za-z0-9-]{12,80}") String requestKey,@Min(1) @Max(1000000) int quantity,@NotBlank @Size(max=80) String lotNumber,@NotNull @PastOrPresent LocalDate receivedAt,@PastOrPresent LocalDate manufacturedAt,LocalDate expiryDate){}
 @GetMapping public Map<String,Object> list(){return service.list();}
 @PostMapping("/quotes") public Object quote(@Valid @RequestBody Quote q){return service.quote(q.productId(),q.supplierId(),q.unitCost(),q.minimumQuantity(),q.leadDays(),q.quotedOn(),q.validUntil(),q.informationConfirmed());}
 @PostMapping public Object draft(@Valid @RequestBody Draft d){return service.create(d.quoteId(),d.quantity(),d.reason());}
 @PostMapping("/{id}/approve") public Object approve(@PathVariable Long id,@Valid @RequestBody ReasonInput r){return service.approve(id,r.reason());}
 @PostMapping("/{id}/cancel") public Object cancel(@PathVariable Long id,@Valid @RequestBody ReasonInput r){return service.cancel(id,r.reason());}
 @PostMapping("/{id}/receive") public Object receive(@PathVariable Long id,@Valid @RequestBody Receipt r){return service.receive(id,r.requestKey(),r.quantity(),r.lotNumber(),r.receivedAt(),r.manufacturedAt(),r.expiryDate());}
}
