package com.aria.config;

import com.aria.service.AriaService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
@EnableScheduling
public class ScheduledTasks {

    private final AriaService ariaService;

    public ScheduledTasks(AriaService ariaService) {
        this.ariaService = ariaService;
    }

    // Запускает анализ каждые 24 часа (86400000 мс)
    @Scheduled(fixedRate = 86400000)
    public void runDailyAriaAnalysis() {
        System.out.println("[SCHEDULER] Запуск автоматического анализа ARIA... " + LocalDateTime.now());
        try {
            AriaService.AnalysisReport report = ariaService.analyze();
            System.out.println("[SCHEDULER] Анализ завершен. Кандидатов: " + report.markedCandidate());
        } catch (Exception e) {
            System.err.println("[SCHEDULER] Ошибка при анализе ARIA: " + e.getMessage());
        }
    }
}