package com.aria.controller;

import com.aria.service.DemoCatalogueImportService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/control/demo-catalogue")
public class DemoCatalogueController {
    private final DemoCatalogueImportService catalogue;
    private final com.aria.service.SupermarketDemoService supermarket;
    public DemoCatalogueController(DemoCatalogueImportService catalogue,com.aria.service.SupermarketDemoService supermarket){this.catalogue=catalogue;this.supermarket=supermarket;}
    public record ImportInput(boolean confirmed) {}
    @GetMapping public Map<String,Object> status(){return catalogue.status();}
    @PostMapping public Map<String,Object> populate(@RequestBody ImportInput input){return catalogue.populate(input.confirmed());}
    @GetMapping("/supermarket") public Map<String,Object> supermarketStatus(){return supermarket.status();}
    @PostMapping("/supermarket") public Map<String,Object> supermarket(@RequestBody ImportInput input){return supermarket.populate(input.confirmed());}
}
