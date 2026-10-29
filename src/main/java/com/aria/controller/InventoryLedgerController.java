package com.aria.controller;
import com.aria.service.InventoryLedgerService;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/control/movements")
public class InventoryLedgerController {
 private final InventoryLedgerService service;public InventoryLedgerController(InventoryLedgerService s){service=s;}
 @GetMapping public Object ledger(@RequestParam(required=false) Long productId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size){return service.report(productId,page,size);}
}
