package com.aria.controller;
import com.aria.service.*;
import com.aria.entity.Product;
import com.aria.dto.ControlRequests.ProductInput;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;

@RestController @RequestMapping("/api/admin/control/products")
public class ProductCreationController {
    private final ProductCreationService service;private final ProductCreationStore store;
    public ProductCreationController(ProductCreationService service,ProductCreationStore store){this.service=service;this.store=store;}
    public record Settings(@Min(0) long revision,boolean textConsentEnabled,@Min(1) @Max(20) int dailyBudget){}
    public record DraftInput(@NotBlank @Size(max=300) String name){}
    public record Confirmation(boolean confirmed,@NotNull @Valid ProductInput product){}
    @GetMapping("/ai-settings") public Map<String,Object> settings(){return service.settings();}
    @PutMapping("/ai-settings") public Map<String,Object> configure(@Valid @RequestBody Settings input){store.configure(input.revision(),input.textConsentEnabled(),input.dailyBudget());return service.settings();}
    @PostMapping("/ai-drafts") public Map<String,Object> generate(@Valid @RequestBody DraftInput input){return service.generate(input.name());}
    @GetMapping("/ai-drafts/{id}") public Map<String,Object> draft(@PathVariable Long id){return store.draft(id);}
    @PostMapping("/ai-drafts/{id}/confirm") public Product confirm(@PathVariable Long id,@Valid @RequestBody Confirmation input){return store.confirm(id,input.confirmed(),input.product());}
    @GetMapping("/{id}/ai-provenance") public Map<String,Object> provenance(@PathVariable Long id){return store.provenance(id);}
}
