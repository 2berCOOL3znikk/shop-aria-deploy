package com.aria;

import com.aria.entity.*;
import com.aria.enums.Role;
import com.aria.repository.AppUserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"aria.smart-pricing.schedule-enabled=false","aria.intelligence.external-enabled=false","aria.seed-demo-catalog=false","aria.seed-demo-users=false","spring.datasource.url=${ARIA_TEST_DATABASE_URL:jdbc:h2:mem:workspacetest;DB_CLOSE_DELAY=-1}","spring.datasource.username=${ARIA_TEST_DATABASE_USER:sa}","spring.datasource.password=${ARIA_TEST_DATABASE_PASSWORD:}","spring.datasource.driver-class-name=${ARIA_TEST_DATABASE_DRIVER:org.h2.Driver}","spring.flyway.enabled=${ARIA_TEST_FLYWAY:false}","spring.flyway.locations=classpath:db/migration/postgresql","spring.flyway.baseline-on-migrate=true","spring.flyway.baseline-version=0","spring.jpa.hibernate.ddl-auto=${ARIA_TEST_SCHEMA_MODE:update}","logging.level.root=ERROR"})
@org.springframework.test.context.TestPropertySource(properties={"aria.manager.enabled=false","aria.manager.schedule-enabled=false","aria.store-sync.worker-enabled=false"})
class WorkspaceOverviewIntegrationTest {
    @Autowired Environment env;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    private final HttpClient http = HttpClient.newHttpClient();
    private final tools.jackson.databind.ObjectMapper json = tools.jackson.databind.json.JsonMapper.builder().build();
    private static final String ENDPOINT = "/api/admin/control/workspace/overview";

    @Test void overviewRequiresAdminAndDoesNotExposeSecrets() throws Exception {
        assertThat(get(null).statusCode()).isEqualTo(401);
        assertThat(get(account(Role.USER)).statusCode()).isEqualTo(403);
        var response = get(account(Role.ADMIN));
        assertThat(response.statusCode()).isEqualTo(200);
        var body = json.readValue(response.body(), Map.class);
        assertThat(body).containsEntry("timeZone", "Asia/Qyzylorda");
        assertThat((Map<String, Object>) body.get("manager")).containsKey("provider");
        assertThat((Map<String, Object>) body.get("manager")).containsEntry("enabled", false);
        assertThat(response.body()).doesNotContain("encryptedCredentials", "ARIA_OPENROUTER_API_KEY", "sk-or-v1-", "deliveryAddress", "ownerEmail", "password", "factsJson", "reportJson");
        assertThat((List<?>) ((Map<?, ?>) body.get("sales")).get("daily")).hasSize(14);
    }

    @Test void dashboardCountsCommittedShippingAndDistinctAiEvidenceWithoutExternalOrdersOrDoubleCounting() throws Exception {
        String admin = account(Role.ADMIN);
        var before = read(admin);
        var now = LocalDateTime.now(ZoneId.of("Asia/Qyzylorda"));
        var marker = "DASHBOARD-" + UUID.randomUUID();
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            var p = new Product(marker, "Товар для проверки обзора", "Тест", 0, now.toLocalDate(), 0, BigDecimal.ONE, BigDecimal.TEN, null);
            em.persist(p);
            nativeOrder(now, "SHIPPED", "1250.50");
            nativeOrder(now.minusDays(1), "DELIVERED", "321.25");
            nativeOrder(now, "CANCELLED", "999999.99");
            nativeOrder(now.plusDays(1), "SHIPPED", "999999.99");

            var store = new ConnectedStore(); store.organization = em.find(Organization.class, 1); store.name = marker; store.platform = "WOOCOMMERCE"; store.baseUrl = "https://test.invalid"; store.status = "CONNECTED"; em.persist(store);
            var external = new StoreOrderSnapshot(); external.storeId = store.id; external.externalId = marker; external.status = "completed"; external.currency = "USD"; external.total = new BigDecimal("888888.88"); external.modifiedAtUtc = now; external.createdAtUtc = now; external.completedAtUtc = now; external.observedAt = now; external.linesJson = "[]"; em.persist(external);

            var ai = decision(p.getId(), now.minusDays(1), "AI"); ai.aiCompletedAt = now; ai.strategy = "KEEP"; em.persist(ai);
            var engine = decision(p.getId(), now, "ENGINE"); engine.status = "PENDING"; engine.aiCompletedAt = now; engine.explanationJson = "{\"mode\":\"ENGINE\"}"; em.persist(engine);
            em.persist(decision(p.getId(), now.minusDays(1), "ENGINE"));
            em.persist(decision(p.getId(), now.plusDays(1), "ENGINE"));
            var textDraft = new ProductAiDraft(); textDraft.inputName = marker; textDraft.status = "DRAFT"; textDraft.name = "Текст для проверки"; textDraft.model = "test/model:free"; textDraft.completedAt = now; em.persist(textDraft);
            var failedText = new ProductAiDraft(); failedText.inputName = marker; failedText.status = "FAILED"; failedText.completedAt = now; em.persist(failedText);
            var futureText = new ProductAiDraft(); futureText.inputName = marker; futureText.status = "DRAFT"; futureText.completedAt = now.plusDays(1); em.persist(futureText);
            var signal = new ManagerAction(); signal.productId = p.getId(); signal.signalKey = marker; signal.type = "LOW_STOCK"; signal.severity = "WARNING"; signal.title = "Мало товара"; signal.reason = "Недостаточный остаток"; signal.fingerprint = marker; signal.firstSeen = now; signal.lastSeen = now; em.persist(signal);
            var proposal = new SocialProposal(); proposal.decisionId = ai.id; proposal.productId = p.getId(); proposal.quantity = 1; proposal.validUntil = now.toLocalDate().plusDays(2); proposal.reason = "Учебный черновик без резерва"; proposal.createdAt = now; em.persist(proposal);
            var manualProposal = new SocialProposal(); manualProposal.decisionId = engine.id; manualProposal.productId = p.getId(); manualProposal.quantity = 1; manualProposal.validUntil = now.toLocalDate().plusDays(2); manualProposal.reason = "Учебный ручной черновик без резерва"; manualProposal.createdAt = now; em.persist(manualProposal);
            var task = new ManagerTask(); task.productId = p.getId(); task.status = "WAITING"; em.persist(task);
            var execution = new ManagerExecution(); execution.taskId = task.id; execution.productId = p.getId(); execution.lease = UUID.randomUUID().toString(); execution.status = "DONE"; execution.socialDraft = true; execution.finishedAt = now; em.persist(execution);
            event(now, "SMART_AUTOPILOT_APPLIED", "decision:" + ai.id, "Тест изменения цены");
            event(now, "DISCOUNT_CHANGED", "product:" + p.getId(), "То же изменение, дополнительная запись");
            event(now, "SMART_AI_DECISION", "decision:" + engine.id, "ENGINE · 0% · проверено");
        });
        var after = read(admin);
        var previousSales = map(before, "sales"); var sales = map(after, "sales");
        assertThat(new BigDecimal(sales.get("revenueToday").toString()).subtract(new BigDecimal(previousSales.get("revenueToday").toString()))).isEqualByComparingTo("1250.50");
        assertThat(number(sales, "ordersToday") - number(previousSales, "ordersToday")).isEqualTo(1);
        assertThat(sales).containsEntry("currency", "KZT").containsEntry("scope", "native");
        var previousToday = map(before, "today"); var today = map(after, "today");
        assertThat(number(today, "productsAnalyzed") - number(previousToday, "productsAnalyzed")).isEqualTo(1);
        assertThat(number(today, "aiDecisions") - number(previousToday, "aiDecisions")).isEqualTo(1);
        assertThat(number(today, "productDrafts") - number(previousToday, "productDrafts")).isEqualTo(1);
        assertThat(number(today, "engineDecisions") - number(previousToday, "engineDecisions")).isEqualTo(1);
        assertThat(number(today, "priceApplied") - number(previousToday, "priceApplied")).isEqualTo(1);
        assertThat(number(today, "socialDrafts") - number(previousToday, "socialDrafts")).isEqualTo(1);
        assertThat(number(today, "problemsFound") - number(previousToday, "problemsFound")).isEqualTo(1);
        assertThat(number(map(after, "risks"), "openSignals") - number(map(before, "risks"), "openSignals")).isEqualTo(1);
        var yesterday = now.toLocalDate().minusDays(1).toString();
        assertThat(dailyRevenue(sales, yesterday).subtract(dailyRevenue(previousSales, yesterday))).isEqualByComparingTo("321.25");
        assertThat(((List<Map<String, Object>>) after.get("recent")).stream().map(e -> e.get("title"))).contains("Сохранён автоматический расчёт");
    }

    private void nativeOrder(LocalDateTime at, String status, String total) {
        var order = new Order(); order.setTotalAmount(new BigDecimal(total)); order.setStatus(status); order.setShippedAt(at); em.persist(order);
    }
    private PricingDecision decision(Long product, LocalDateTime at, String source) {
        // Historical rows are closed: PostgreSQL permits only one PENDING decision per product.
        var d = new PricingDecision(); d.status = "SUPERSEDED"; d.productId = product; d.createdAt = at; d.reviewAt = at.plusHours(48); d.source = source; d.fingerprint = UUID.randomUUID().toString(); d.reportJson = "{}"; return d;
    }
    private void event(LocalDateTime at, String action, String resource, String details) {
        var event = new AuditEvent(); event.setCreatedAt(at); event.setActor("workspace-test"); event.setAction(action); event.setResource(resource); event.setDetails(details); em.persist(event);
    }
    private String account(Role role) throws Exception {
        var email = "workspace-" + UUID.randomUUID() + "@test.local";
        users.save(new AppUser(email, passwords.encode("workspaceTest123"), "Проверка", role));
        var request = HttpRequest.newBuilder(uri("/api/auth/login")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("email", email, "password", "workspaceTest123")))).build();
        var response = http.send(request, HttpResponse.BodyHandlers.ofString()); assertThat(response.statusCode()).isEqualTo(200); return (String) json.readValue(response.body(), Map.class).get("token");
    }
    private Map<String, Object> read(String admin) throws Exception { var response = get(admin); assertThat(response.statusCode()).as(response.body()).isEqualTo(200); return json.readValue(response.body(), Map.class); }
    private HttpResponse<String> get(String token) throws Exception { var builder = HttpRequest.newBuilder(uri(ENDPOINT)); if (token != null) builder.header("Authorization", "Bearer " + token); return http.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString()); }
    private URI uri(String path) { return URI.create("http://localhost:" + env.getProperty("local.server.port") + path); }
    private static Map<String, Object> map(Map<String, Object> parent, String key) { return (Map<String, Object>) parent.get(key); }
    private static long number(Map<String, Object> parent, String key) { return ((Number) parent.get(key)).longValue(); }
    private static BigDecimal dailyRevenue(Map<String, Object> sales, String date) { return ((List<Map<String, Object>>) sales.get("daily")).stream().filter(point -> date.equals(point.get("date").toString())).map(point -> new BigDecimal(point.get("revenue").toString())).findFirst().orElseThrow(); }
}
