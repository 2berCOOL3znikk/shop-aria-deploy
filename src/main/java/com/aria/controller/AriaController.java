package com.aria.controller;

import com.aria.entity.TransferLot;
import com.aria.service.AriaService;
import com.aria.service.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/aria")
public class AriaController {

    private final AriaService aria;
    private final StatsService statsService;

    public AriaController(AriaService aria, StatsService statsService) {
        this.aria = aria;
        this.statsService = statsService;
    }

    @PostMapping("/analyze")
    public AriaService.AnalysisReport analyze() {
        return aria.analyze();
    }

    @PostMapping("/reset-statuses")
    public Map<String, Object> resetStatuses() {
        int count = aria.resetAllStatusesToActive();
        Map<String, Object> res = new HashMap<>();
        res.put("resetCount", count);
        res.put("message", "Статусы " + count + " товаров сброшены в АКТИВЕН. Теперь можно запустить анализ заново.");
        return res;
    }

    @PostMapping("/transfer")
    public TransferLot transfer(
            @RequestParam(defaultValue = "Фонд Тёплый дом") String partner,
            @RequestParam(required = false) String note) {
        return aria.createTransfer(partner, note);
    }

    @GetMapping("/act/{lotId}")
    public ResponseEntity<String> act(@PathVariable Long lotId) {
        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=\"aria-act-" + lotId + ".html\"")
                .body(aria.generateActHtml(lotId));
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return statsService.getEconomicReport();
    }
}