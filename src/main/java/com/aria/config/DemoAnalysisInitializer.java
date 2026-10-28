package com.aria.config;

import com.aria.repository.ProductRepository;
import com.aria.service.AriaService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="aria.seed-demo-catalog", havingValue="true")
public class DemoAnalysisInitializer {
    private final ProductRepository products;
    private final AriaService aria;
    private final com.aria.service.InventoryService inventory;

    public DemoAnalysisInitializer(ProductRepository products, AriaService aria, com.aria.service.InventoryService inventory) {
        this.products = products;
        this.aria = aria;
        this.inventory=inventory;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void analyzeDemoCatalog() {
        if (products.count() == 0) return;
        inventory.initializeExisting();
        aria.analyze();
    }
}
