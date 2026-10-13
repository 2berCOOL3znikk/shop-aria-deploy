package com.aria.service;

import com.aria.entity.AriaLog;
import com.aria.entity.Product;
import com.aria.enums.ProductStatus;
import com.aria.repository.AriaLogRepository;
import com.aria.repository.ProductRepository;
import com.aria.repository.TransferLotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AriaServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private TransferLotRepository transferLotRepository;

    @Mock
    private CompetitorAnalyzerService competitorAnalyzerService;

    @Mock
    private AriaLogRepository ariaLogRepository;

    @InjectMocks
    private AriaService ariaService;

    @Test
    void analyze_marksDeadStockAsCandidate() {
        // given: товар >180 дней без продаж
        Product deadStock = new Product();
        deadStock.setId(1L);
        deadStock.setName("Мёртвый товар");
        deadStock.setReceivedAt(LocalDate.now().minusDays(200));
        deadStock.setSalesLast30Days(0);
        deadStock.setStatus(ProductStatus.ACTIVE);
        deadStock.setMarketAvgPrice(new BigDecimal("100"));

        when(productRepository.findAll()).thenReturn(List.of(deadStock));
        when(competitorAnalyzerService.findMarketAveragePrice(any(), any())).thenReturn(new BigDecimal("100"));

        // when
        AriaService.AnalysisReport report = ariaService.analyze();

        // then
        assertThat(report.markedCandidate()).isEqualTo(1);
        assertThat(deadStock.getStatus()).isEqualTo(ProductStatus.CANDIDATE);
        assertThat(deadStock.getAriaReason()).contains("не продаётся");
    }

    @Test
    void analyze_marksExpiringProductAsCandidateAndUrgent() {
        // given: товар со сроком <30 дней
        Product expiring = new Product();
        expiring.setId(2L);
        expiring.setName("Скоропортящийся товар");
        expiring.setReceivedAt(LocalDate.now());
        expiring.setSalesLast30Days(5);
        expiring.setExpiryDate(LocalDate.now().plusDays(15));
        expiring.setStatus(ProductStatus.ACTIVE);
        expiring.setMarketAvgPrice(new BigDecimal("50"));

        when(productRepository.findAll()).thenReturn(List.of(expiring));
        when(competitorAnalyzerService.findMarketAveragePrice(any(), any())).thenReturn(new BigDecimal("50"));

        // when
        AriaService.AnalysisReport report = ariaService.analyze();

        // then
        assertThat(report.markedCandidate()).isEqualTo(1);
        assertThat(report.urgent()).isEqualTo(1);
        assertThat(expiring.getStatus()).isEqualTo(ProductStatus.CANDIDATE);
        assertThat(expiring.getAriaReason()).contains("срок годности");
    }

    @Test
    void analyze_returnsCleanCandidateToActive() {
        // given: бывший кандидат без причин
        Product formerCandidate = new Product();
        formerCandidate.setId(3L);
        formerCandidate.setName("Восстановленный товар");
        formerCandidate.setReceivedAt(LocalDate.now().minusDays(10));
        formerCandidate.setSalesLast30Days(10);
        formerCandidate.setStatus(ProductStatus.CANDIDATE);
        formerCandidate.setMarketAvgPrice(new BigDecimal("200"));

        when(productRepository.findAll()).thenReturn(List.of(formerCandidate));
        when(competitorAnalyzerService.findMarketAveragePrice(any(), any())).thenReturn(new BigDecimal("200"));

        // when
        AriaService.AnalysisReport report = ariaService.analyze();

        // then
        assertThat(report.returnedToActive()).isEqualTo(1);
        assertThat(formerCandidate.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(formerCandidate.getAriaReason()).isNull();
    }

    @Test
    void analyze_detectsNegativeMargin() {
        // given: товар с отрицательной маржой
        Product negativeMargin = new Product();
        negativeMargin.setId(4L);
        negativeMargin.setName("Убыточный товар");
        negativeMargin.setReceivedAt(LocalDate.now());
        negativeMargin.setSalesLast30Days(5);
        negativeMargin.setUnitCost(new BigDecimal("150"));
        negativeMargin.setUnitPrice(new BigDecimal("100"));
        negativeMargin.setStatus(ProductStatus.ACTIVE);
        negativeMargin.setMarketAvgPrice(new BigDecimal("100"));

        when(productRepository.findAll()).thenReturn(List.of(negativeMargin));
        when(competitorAnalyzerService.findMarketAveragePrice(any(), any())).thenReturn(new BigDecimal("100"));

        // when
        AriaService.AnalysisReport report = ariaService.analyze();

        // then
        assertThat(report.markedCandidate()).isEqualTo(1);
        assertThat(negativeMargin.getStatus()).isEqualTo(ProductStatus.CANDIDATE);
        assertThat(negativeMargin.getAriaReason()).contains("отрицательная маржа");
    }

    @Test
    void analysisReport_countersAreCorrect() {
        // given: несколько товаров с разными проблемами
        List<Product> products = new ArrayList<>();

        Product deadStock = new Product();
        deadStock.setId(1L);
        deadStock.setReceivedAt(LocalDate.now().minusDays(200));
        deadStock.setSalesLast30Days(0);
        deadStock.setStatus(ProductStatus.ACTIVE);
        deadStock.setMarketAvgPrice(new BigDecimal("100"));
        products.add(deadStock);

        Product expiring = new Product();
        expiring.setId(2L);
        expiring.setReceivedAt(LocalDate.now());
        expiring.setSalesLast30Days(5);
        expiring.setExpiryDate(LocalDate.now().plusDays(15));
        expiring.setStatus(ProductStatus.ACTIVE);
        expiring.setMarketAvgPrice(new BigDecimal("50"));
        products.add(expiring);

        Product priceOutlier = new Product();
        priceOutlier.setId(3L);
        priceOutlier.setReceivedAt(LocalDate.now());
        priceOutlier.setSalesLast30Days(5);
        priceOutlier.setUnitPrice(new BigDecimal("150"));
        priceOutlier.setStatus(ProductStatus.ACTIVE);
        priceOutlier.setMarketAvgPrice(new BigDecimal("100"));
        products.add(priceOutlier);

        when(productRepository.findAll()).thenReturn(products);
        when(competitorAnalyzerService.findMarketAveragePrice(any(), any())).thenReturn(new BigDecimal("100"));
        when(ariaLogRepository.save(any(AriaLog.class))).thenReturn(new AriaLog());

        // when
        AriaService.AnalysisReport report = ariaService.analyze();

        // then
        assertThat(report.markedCandidate()).isEqualTo(3);
        assertThat(report.urgent()).isEqualTo(1);
        assertThat(report.priceOutliers()).isEqualTo(1);
        assertThat(report.totalScanned()).isEqualTo(3);

        verify(ariaLogRepository, times(1)).save(any(AriaLog.class));
    }
}
