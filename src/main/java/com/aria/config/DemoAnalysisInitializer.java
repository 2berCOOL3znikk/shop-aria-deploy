package com.aria.config;

import com.aria.repository.ProductRepository;
import com.aria.service.AriaService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DemoAnalysisInitializer {
    private final ProductRepository products;
    private final AriaService aria;

    public DemoAnalysisInitializer(ProductRepository products, AriaService aria) {
        this.products = products;
        this.aria = aria;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void analyzeDemoCatalog() {
        if (products.count() == 0) return;
        aria.analyze();
    }
}