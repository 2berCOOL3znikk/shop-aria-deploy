package com.aria.config;

import com.aria.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FreeHostingTest {
    @Test void freeProfileReleasesConnectionsAndDoesNotProbeSleepingDatabase()throws Exception {
        var properties=PropertiesLoaderUtils.loadProperties(new ClassPathResource("application-free.properties"));
        var hikari=new com.zaxxer.hikari.HikariConfig();
        hikari.setMaximumPoolSize(Integer.parseInt(properties.getProperty("spring.datasource.hikari.maximum-pool-size")));
        hikari.setMinimumIdle(Integer.parseInt(properties.getProperty("spring.datasource.hikari.minimum-idle")));
        hikari.setIdleTimeout(Long.parseLong(properties.getProperty("spring.datasource.hikari.idle-timeout")));
        hikari.setKeepaliveTime(Long.parseLong(properties.getProperty("spring.datasource.hikari.keepalive-time")));
        assertThat(hikari.getMaximumPoolSize()).isEqualTo(3);
        assertThat(hikari.getMinimumIdle()).isZero();
        assertThat(hikari.getKeepaliveTime()).isZero();
        assertThat(hikari.getIdleTimeout()).isLessThan(300000);
        assertThat(properties.getProperty("management.health.db.enabled")).isEqualTo("false");
    }
    @Test void freeBackgroundTasksDoNotOpenDatabaseConnections() {
        var inventory=mock(InventoryService.class);var analysis=mock(AriaService.class);
        var tasks=new ScheduledTasks(analysis,inventory);
        ReflectionTestUtils.setField(tasks,"stockRefreshEnabled",false);
        ReflectionTestUtils.setField(tasks,"backgroundAnalysisEnabled",false);
        tasks.refreshAvailableStock();tasks.runDailyAriaAnalysis();
        verifyNoInteractions(inventory,analysis);
    }
    @Test void freeIntelligenceUsesFactsEvenWhenAnExternalApiKeyExists() {
        var service=new IntelligenceService("unused-test-key","unused-test-model");
        ReflectionTestUtils.setField(service,"externalEnabled",false);
        var response=service.explain(java.util.Map.of("briefing",java.util.List.of("Проверенный остаток: 3")));
        assertThat(response).containsEntry("mode","RULES").containsEntry("configured",false).containsEntry("text","Проверенный остаток: 3");
    }
    @Test void unavailableDatabaseProducesRetryableResponseWithoutLeakingConnectionDetails() {
        var result=new GlobalExceptionHandler().handleStorageUnavailable(new org.springframework.dao.DataAccessResourceFailureException("secret-connection-url"));
        assertThat(result.getStatusCode().value()).isEqualTo(503);
        assertThat(result.getHeaders().getFirst("Retry-After")).isEqualTo("30");
        assertThat(result.getBody().toString()).doesNotContain("secret-connection-url");
    }
}
