package com.aria.controller;

import com.aria.service.DemoCatalogueImportService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/control/demo-catalogue")
public class DemoCatalogueController {
    private final DemoCatalogueImportService catalogue;
    public DemoCatalogueController(DemoCatalogueImportService catalogue){this.catalogue=catalogue;}
    public record ImportInput(boolean confirmed) {}
    @GetMapping public Map<String,Object> status(){return catalogue.status();}
    @PostMapping public Map<String,Object> populate(@RequestBody ImportInput input){return catalogue.populate(input.confirmed());}
}
