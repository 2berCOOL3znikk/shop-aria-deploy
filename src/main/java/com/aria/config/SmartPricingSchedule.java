package com.aria.config;
import com.aria.service.SmartPricingService;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;

@Component
public class SmartPricingSchedule {
    private final SmartPricingService smart;
    @Value("${aria.smart-pricing.schedule-enabled:true}") private boolean enabled;
    public SmartPricingSchedule(SmartPricingService smart){this.smart=smart;}
    @Scheduled(cron="${aria.smart-pricing.cron:0 0 8 * * *}",zone="Asia/Qyzylorda")
    public void daily(){if(enabled)smart.analyzeAll(true);}
}
