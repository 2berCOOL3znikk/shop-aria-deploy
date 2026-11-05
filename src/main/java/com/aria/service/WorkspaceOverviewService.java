package com.aria.service;

import com.aria.entity.*;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Read-only dashboard. External snapshots never become native sales or inventory. */
@Service
@Transactional(readOnly = true)
public class WorkspaceOverviewService {
    static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Qyzylorda");
    private final EntityManager em;
    private final AdminReadService adminRead;
    private final IntelligenceService intelligence;
    @Value("${aria.manager.enabled:true}") private boolean managerEnabled;
    @Value("${aria.manager.schedule-enabled:true}") private boolean managerScheduled;
    @Value("${aria.manager.activity-enabled:true}") private boolean managerActivity;

    public WorkspaceOverviewService(EntityManager em, AdminReadService adminRead, IntelligenceService intelligence) {
        this.em = em;
        this.adminRead = adminRead;
        this.intelligence = intelligence;
    }

    public Map<String, Object> overview() {
        var now = LocalDateTime.now(BUSINESS_ZONE);
        var today = now.toLocalDate();
        var from = today.atStartOfDay();
        var until = now;
        var result = new LinkedHashMap<String, Object>();
        result.put("asOf", now);
        result.put("date", today);
        result.put("timeZone", BUSINESS_ZONE.getId());
        var organization = em.find(Organization.class, 1);
        result.put("organization", Map.of("name", organization.name, "demonstration", organization.demonstration));
        var inventory = adminRead.overview();
        var compact = new LinkedHashMap<String, Object>();
        for (var key : List.of("products", "availableUnits", "lowStock", "pendingOrders", "expiredLots", "urgentLots")) compact.put(key, inventory.get(key));
        result.put("overview", compact);

        result.put("stores", Map.of(
                "total", count("ConnectedStore s", "s.archived=false and s.testConnection=false and s.status<>'DISCONNECTED'"),
                "connected", count("ConnectedStore s", "s.archived=false and s.testConnection=false and s.status='CONNECTED'"),
                "attention", count("ConnectedStore s", "s.archived=false and s.testConnection=false and s.status not in ('CONNECTED','DISCONNECTED')"),
                "catalogCards", count("StoreSnapshot s", "s.storeId in (select c.id from ConnectedStore c where c.archived=false and c.testConnection=false and c.status<>'DISCONNECTED')")
                        + (count("ConnectedStore s", "s.archived=false and s.platform='ARIA_DEMO' and s.status<>'DISCONNECTED'") > 0 ? ((Number) inventory.get("products")).longValue() : 0L)));

        var series = new LinkedHashMap<LocalDate, Map<String, Object>>();
        for (int day = 13; day >= 0; day--) series.put(today.minusDays(day), new LinkedHashMap<>(Map.of("date", today.minusDays(day), "revenue", "0.00", "orders", 0L)));
        // Aggregate by the recorded shipping date, never order creation or an invented trend.
        var salesRows = em.createQuery("select year(o.shippedAt),month(o.shippedAt),day(o.shippedAt),sum(o.totalAmount),count(o) from Order o where o.shippedAt>=:from and o.shippedAt<:until and o.status in ('SHIPPED','DELIVERED') group by year(o.shippedAt),month(o.shippedAt),day(o.shippedAt)", Object[].class)
                .setParameter("from", today.minusDays(13).atStartOfDay()).setParameter("until", until).getResultList();
        for (var row : salesRows) {
            var date = LocalDate.of(((Number) row[0]).intValue(), ((Number) row[1]).intValue(), ((Number) row[2]).intValue());
            var point = series.get(date);
            if (point != null) { point.put("revenue", ((BigDecimal) row[3]).toPlainString()); point.put("orders", row[4]); }
        }
        result.put("sales", Map.of("revenueToday", series.get(today).get("revenue"), "ordersToday", series.get(today).get("orders"), "daily", series.values(), "currency", "KZT", "scope", "native"));

        var actions = new LinkedHashMap<String, Object>();
        actions.put("productsAnalyzed", during("select count(p) from Product p where p.id in (select d.productId from PricingDecision d where (d.createdAt>=:from and d.createdAt<:until) or (d.aiCompletedAt>=:from and d.aiCompletedAt<:until)) or p.id in (select e.productId from ManagerExecution e where e.status='DONE' and e.finishedAt>=:from and e.finishedAt<:until)", from, until));
        actions.put("aiDecisions", during("select count(d) from PricingDecision d where d.source='AI' and d.aiCompletedAt>=:from and d.aiCompletedAt<:until", from, until));
        actions.put("productDrafts", during("select count(d) from ProductAiDraft d where d.source='AI' and d.status in ('DRAFT','CONFIRMED') and d.completedAt>=:from and d.completedAt<:until", from, until));
        actions.put("engineDecisions", during("select count(d) from PricingDecision d where d.source='ENGINE' and d.createdAt>=:from and d.createdAt<:until", from, until));
        actions.put("priceApplied", during("select count(e) from AuditEvent e where e.action='SMART_AUTOPILOT_APPLIED' and e.createdAt>=:from and e.createdAt<:until", from, until));
        actions.put("purchaseDrafts", during("select count(p) from PurchaseRequest p where p.managerCreated=true and p.createdAt>=:from and p.createdAt<:until", from, until));
        actions.put("socialDrafts", during("select count(e) from ManagerExecution e where e.status='DONE' and e.socialDraft=true and e.finishedAt>=:from and e.finishedAt<:until", from, until));
        actions.put("problemsFound", during("select count(a) from ManagerAction a where a.firstSeen>=:from and a.firstSeen<:until", from, until));
        long connectedAnalyzed=em.createQuery("select distinct d.storeId,d.externalId from StorePricingDecision d where d.createdAt>=:from and d.createdAt<:until or d.aiCompletedAt>=:from and d.aiCompletedAt<:until",Object[].class).setParameter("from",from).setParameter("until",until).getResultList().size();
        actions.put("productsAnalyzed",((Number)actions.get("productsAnalyzed")).longValue()+connectedAnalyzed);
        actions.put("aiDecisions",((Number)actions.get("aiDecisions")).longValue()+during("select count(d) from StorePricingDecision d where d.source='AI' and d.aiCompletedAt>=:from and d.aiCompletedAt<:until",from,until));
        actions.put("engineDecisions",((Number)actions.get("engineDecisions")).longValue()+during("select count(d) from StorePricingDecision d where d.source='ENGINE' and d.createdAt>=:from and d.createdAt<:until",from,until));
        actions.put("priceApplied",((Number)actions.get("priceApplied")).longValue()+during("select count(c) from StorePriceChange c where c.automatic=true and c.status='APPLIED' and c.completedAt>=:from and c.completedAt<:until",from,until));
        result.put("today", actions);
        result.put("risks", Map.of("openSignals", count("ManagerAction a", "a.status='OPEN'")+ConnectedPricingService.attentionCount(em),
                "pendingDecisions", em.createQuery("select count(d) from PricingDecision d where d.status='PENDING' and d.reviewAt>:now", Long.class).setParameter("now", now).getSingleResult(),
                "failedTasks", count("ManagerTask t", "t.status='FAILED'")));

        var state = em.find(AiManagerState.class, 1L);
        var pricing = em.find(SmartPricingSettings.class, 1L);
        var manager = new LinkedHashMap<String, Object>();
        manager.put("mode", state == null ? "ADVISOR" : state.mode);
        manager.put("enabled", managerEnabled && state != null && !"OFF".equals(state.mode) && (managerScheduled || managerActivity));
        manager.put("scheduledAvailable", managerScheduled);
        manager.put("activityAvailable", managerActivity);
        manager.put("automaticAiConsent", state != null && state.automaticAiConsent);
        manager.put("externalAiConsent", pricing != null && pricing.externalAiEnabled);
        manager.put("scheduledAiConsent", pricing != null && pricing.scheduledAiEnabled);
        manager.put("lastFinished", state == null ? null : state.lastFinished);
        manager.put("lastError", state == null ? null : state.lastError);
        var provider = new LinkedHashMap<String, Object>();
        var status = intelligence.pricingStatus();
        for (var key : List.of("configured", "status", "lastAttempt", "lastSuccess")) provider.put(key, status.get(key));
        manager.put("provider", provider);
        result.put("manager", manager);
        result.put("recent", em.createQuery("select e from AuditEvent e order by e.createdAt desc,e.id desc", AuditEvent.class).setMaxResults(8).getResultList().stream().map(this::event).toList());
        return result;
    }

    private long count(String entity, String where) {
        return em.createQuery("select count(*) from " + entity + " where " + where, Long.class).getSingleResult();
    }
    private long during(String query, LocalDateTime from, LocalDateTime until) {
        return em.createQuery(query, Long.class).setParameter("from", from).setParameter("until", until).getSingleResult();
    }
    private Map<String, Object> event(AuditEvent event) {
        String action = event.getAction(), title, section;
        if (action.startsWith("STORE_")) { title = switch (action) { case "STORE_CREATED" -> "Добавлен магазин"; case "STORE_SYNCED" -> "Обновлён каталог магазина"; case "STORE_ORDERS_SYNCED" -> "Обновлены заказы магазина"; case "STORE_DISCONNECTED" -> "Магазин отключён"; case "STORE_SYNC_FAILED", "STORE_ORDERS_SYNC_FAILED" -> "Подключение требует внимания"; default -> "Обновлены настройки магазина"; }; section = "stores"; }
        else if (action.startsWith("PRODUCT_")) { title = action.equals("PRODUCT_CREATED") ? "Создан товар" : action.equals("PRODUCT_ARCHIVED") ? "Товар архивирован" : "Обновлена карточка товара"; section = "products"; }
        else if (action.startsWith("SMART_")) { title = switch (action) { case "SMART_AI_DECISION" -> event.getDetails() != null && event.getDetails().startsWith("AI ·") ? "ARIA AI выбрала стратегию" : "Сохранён автоматический расчёт"; case "SMART_PRICING_RECOMMENDED" -> "Подготовлена рекомендация по цене"; case "SMART_AUTOPILOT_APPLIED" -> "Автоматически изменена цена"; case "SMART_PRICING_APPLIED" -> "Подтверждена новая цена"; case "SMART_AUTOPILOT_SKIPPED" -> "Цена сохранена после проверки ограничений"; default -> "Обновлено решение по цене"; }; section = "smart"; }
        else if (action.startsWith("ORDER_") || action.startsWith("POS_")) { title = action.equals("ORDER_CREATED") ? "Получен заказ" : "Обновлён заказ"; section = "orders"; }
        else if (action.startsWith("LOT_") || action.startsWith("INVENTORY_")) { title = "Обновлены складские данные"; section = "lots"; }
        else if (action.startsWith("PURCHASE_")) { title = "Обновлена закупка"; section = "purchases"; }
        else if (action.startsWith("SOCIAL_")) { title = action.equals("SOCIAL_PROPOSED") ? "Подготовлено социальное предложение" : "Обновлена социальная передача"; section = "socialPlans"; }
        else if (action.startsWith("MANAGER_")) { title = switch (action) { case "MANAGER_CYCLE_STARTED" -> "ARIA начала проверку магазина"; case "MANAGER_CYCLE_FINISHED" -> "ARIA завершила цикл проверки"; default -> "Обновлены настройки ARIA"; }; section = "manager"; }
        else { title = "В журнале появилось новое действие"; section = "audit"; }
        // No raw technical action names, JSON, customer contacts or audit details in the dashboard.
        return Map.of("at", event.getCreatedAt(), "title", title, "section", section);
    }
}
