package com.aria.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;

/** Legacy API retained for compatibility. Only dated, matched CompetitorQuote rows are market evidence. */
@Service
public class CompetitorAnalyzerService {
    public BigDecimal findMarketAveragePrice(String productName,String category) {
        // No network source is configured here. A demo flag must never manufacture a market observation.
        return null;
    }
}
