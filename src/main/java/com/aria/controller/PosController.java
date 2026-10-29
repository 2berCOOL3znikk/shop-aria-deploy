package com.aria.controller;
import com.aria.service.PosService;
import com.aria.service.integrations.PosAdapter;
import com.aria.dto.ControlRequests.ReasonInput;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.dao.DataIntegrityViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import java.math.BigDecimal;
@RestController @RequestMapping("/api/admin/control/pos")
public class PosController {
 private final PosService service;public PosController(PosService s){service=s;}
 public record Line(@NotNull @Positive Long productId,@Min(1) @Max(1000000) int quantity,@NotNull @DecimalMin("0.01") @DecimalMax("9999999999.99") @Digits(integer=10,fraction=2) BigDecimal expectedPrice){}
 public record Sale(@NotBlank @Pattern(regexp="[A-Za-z0-9-]{12,80}") String requestKey,@NotNull @Size(min=1,max=100) List<@NotNull @Valid Line> lines){}
 @GetMapping public Object list(){return service.list();}
 @PostMapping("/sales") public Object sale(@Valid @RequestBody Sale input,Authentication user){var sale=new PosAdapter.Sale(input.requestKey(),input.lines().stream().map(l->new PosAdapter.Line(l.productId(),l.quantity(),l.expectedPrice())).toList());try{return service.record(sale,user.getName());}catch(DataIntegrityViolationException duplicate){return service.replay(sale,user.getName());}}
 @PostMapping("/{id}/return") public Object returned(@PathVariable Long id,@Valid @RequestBody ReasonInput reason){return service.returnToQuarantine(id,reason.reason());}
}
