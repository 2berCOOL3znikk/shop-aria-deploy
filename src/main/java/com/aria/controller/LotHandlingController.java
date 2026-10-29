package com.aria.controller;
import com.aria.service.InventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/control/lots")
public class LotHandlingController {
 private final InventoryService inventory;public LotHandlingController(InventoryService i){inventory=i;}
 public record Evidence(@Min(0) long revision,@NotBlank @Size(max=700) String requirements,@NotBlank @Pattern(regexp="UNKNOWN|CONFIRMED|REJECTED") String status,boolean informationConfirmed,@NotBlank @Size(max=700) String reason){}
 @PutMapping("/{id}/handling") public Object handling(@PathVariable Long id,@Valid @RequestBody Evidence e){return inventory.handling(id,e.revision(),e.requirements(),e.status(),e.informationConfirmed(),e.reason());}
}
