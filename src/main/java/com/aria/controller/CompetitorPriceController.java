package com.aria.controller;
import com.aria.service.CompetitorPriceService;
import com.aria.entity.CompetitorSource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/admin/control/competitors")
public class CompetitorPriceController {
    private final CompetitorPriceService service;
    private final com.aria.service.integrations.CompetitorImportAdapter adapter;
    public CompetitorPriceController(CompetitorPriceService service,com.aria.service.integrations.CompetitorImportAdapter adapter){this.service=service;this.adapter=adapter;}
    public record SourceInput(@NotBlank @Size(max=160) String name,@NotBlank @Size(max=500) String baseUrl,boolean active,@AssertTrue boolean permissionConfirmed){}
    public record ImportInput(@NotNull Long sourceId,@NotBlank @Size(max=200000) String csv,@AssertTrue boolean identityConfirmed){}
    @GetMapping public List<CompetitorSource> list(){return service.sources();}
    @PostMapping public CompetitorSource create(@Valid @RequestBody SourceInput in){return service.saveSource(null,in.name(),in.baseUrl(),in.active());}
    @PutMapping("/{id}") public CompetitorSource update(@PathVariable Long id,@Valid @RequestBody SourceInput in){return service.saveSource(id,in.name(),in.baseUrl(),in.active());}
    @PostMapping("/import") public Map<String,Object> load(@Valid @RequestBody ImportInput in){return adapter.importSnapshot(new com.aria.service.integrations.CompetitorImportAdapter.Snapshot(in.sourceId(),in.csv(),in.identityConfirmed()));}
    @GetMapping("/products/{id}") public Map<String,Object> product(@PathVariable Long id){return service.product(id);}
}
