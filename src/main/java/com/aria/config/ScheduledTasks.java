package com.aria.config;

import com.aria.service.AriaService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
@EnableScheduling
public class ScheduledTasks {
    @org.springframework.beans.factory.annotation.Value("${aria.stock-refresh-enabled:true}")
    private boolean stockRefreshEnabled;
    @org.springframework.beans.factory.annotation.Value("${aria.background-analysis-enabled:true}")
    private boolean backgroundAnalysisEnabled;

    private final AriaService ariaService;
    private final com.aria.service.InventoryService inventory;

    public ScheduledTasks(AriaService ariaService,com.aria.service.InventoryService inventory) {
        this.ariaService = ariaService;
        this.inventory=inventory;
    }
    @Scheduled(fixedDelay=300000,initialDelay=300000)
    public void refreshAvailableStock(){if(stockRefreshEnabled)inventory.initializeExisting();}

    // Запускает анализ каждые 24 часа (86400000 мс)
    @Scheduled(fixedRate = 86400000)
    public void runDailyAriaAnalysis() {
        if(!backgroundAnalysisEnabled)return;
        System.out.println("[SCHEDULER] Запуск автоматического анализа ARIA... " + LocalDateTime.now());
        try {
            AriaService.AnalysisReport report = ariaService.analyze();
            System.out.println("[SCHEDULER] Анализ завершен. Кандидатов: " + report.markedCandidate());
        } catch (Exception e) {
            System.err.println("[SCHEDULER] Ошибка при анализе ARIA: " + e.getMessage());
        }
    }
}
