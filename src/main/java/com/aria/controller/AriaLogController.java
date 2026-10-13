package com.aria.controller;

import com.aria.entity.AriaLog;
import com.aria.repository.AriaLogRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/aria/logs")
public class AriaLogController {

    private final AriaLogRepository logRepo;

    public AriaLogController(AriaLogRepository logRepo) {
        this.logRepo = logRepo;
    }

    @GetMapping
    public List<AriaLog> getAllLogs() {
        return logRepo.findAllByOrderByAnalyzedAtDesc();
    }
}