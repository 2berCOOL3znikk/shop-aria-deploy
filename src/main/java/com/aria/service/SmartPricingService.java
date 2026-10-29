package com.aria.service;

import com.aria.dto.ControlRequests.DiscountInput;
import com.aria.entity.*;
import com.aria.repository.*;
import com.aria.service.pricing.SmartPricingEngine;
import com.aria.service.pricing.SmartPricingEngine.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class SmartPricingService {
    private final ProductRepository products;private final InventoryLotRepository lots;private final OrderRepository orders;
    private final PricingDecisionRepository decisions;private final PriceChangeRepository changes;
    private final InventoryService inventory;private final PricingService pricing;private final PolicyService policies;
    private final ControlService control;private final AuditService audit;private final TransactionTemplate transaction;
    private final SmartPricingSettingsRepository settings;
    private final CompetitorPriceService competitors;
    private final CharityEligibilityService charities;
    @org.springframework.beans.factory.annotation.Autowired private AiManagerStateRepository managerStates;
    private final tools.jackson.databind.ObjectMapper json=tools.jackson.databind.json.JsonMapper.builder().enable(tools.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).build();
    public SmartPricingService(ProductRepository products,InventoryLotRepository lots,OrderRepository orders,
        PricingDecisionRepository decisions,PriceChangeRepository changes,InventoryService inventory,PricingService pricing,
        PolicyService policies,ControlService control,AuditService audit,PlatformTransactionManager manager,SmartPricingSettingsRepository settings,CompetitorPriceService competitors,CharityEligibilityService charities){
        this.products=products;this.lots=lots;this.orders=orders;this.decisions=decisions;this.changes=changes;
        this.inventory=inventory;this.pricing=pricing;this.policies=policies;this.control=control;this.audit=audit;this.transaction=new TransactionTemplate(manager);
        this.settings=settings;
        this.competitors=competitors;
        this.charities=charities;
    }
    public Map<String,Object> analyzeAll(boolean onlyDue){
        int analyzed=0,failed=0;
        for(var product:products.findAll().stream().filter(this::eligible).sorted(Comparator.comparing(Product::getId)).toList()){
            // Time alone is insufficient: stock, sales or imported prices can change before reviewAt.
            // analyzeLocked compares the full current fingerprint and keeps an unchanged decision idempotent.
            try{transaction.execute(s->analyzeLocked(product.getId()));analyzed++;}catch(RuntimeException e){failed++;org.slf4j.LoggerFactory.getLogger(SmartPricingService.class).warn("Smart Pricing calculation failed for product {} ({})",product.getId(),e.getClass().getSimpleName());}
        }
        return Map.of("analyzed",analyzed,"failed",failed,"mode",settings.findById(1L).map(s->s.mode).orElse("RECOMMEND"));
    }
    @Transactional public Map<String,Object> analyze(Long productId){return view(analyzeLocked(productId));}
    @Transactional public Map<String,Object> analyzeForManager(Long productId){return view(analyzeLocked(productId,false));}
    private PricingDecision analyzeLocked(Long productId){
        return analyzeLocked(productId,true);
    }
    private PricingDecision analyzeLocked(Long productId,boolean allowAutopilot){
        Product p=inventory.lock(productId);if(!eligible(p))throw new IllegalStateException("Товар недоступен для ценообразования");
        var existingLots=lots.findByProductId(productId);
        if(existingLots.isEmpty()&&p.getQuantityOnHand()>0&&p.getUnitCost()==null)
            throw new IllegalStateException("Неизвестна себестоимость начального остатка. Укажите закупочную цену партии перед анализом");
        boolean food=p.getCategory()!=null&&p.getCategory().toLowerCase(Locale.ROOT).contains("продукт");
        if(food&&(existingLots.isEmpty()?p.getQuantityOnHand()>0&&p.getExpiryDate()==null:existingLots.stream().anyMatch(l->l.getActive()&&l.getQuantity()>0&&l.getExpiryDate()==null)))
            throw new IllegalStateException("У пищевой партии не указан срок годности. Уточните пригодность перед анализом");
        inventory.ensureLegacyLot(p);inventory.synchronize(p);
        var report=SmartPricingEngine.evaluate(facts(p));var fingerprint=fingerprint(report);
        var last=decisions.findFirstByProductIdOrderByCreatedAtDescIdDesc(productId);
        if(last.isPresent()&&last.get().fingerprint.equals(fingerprint)&&last.get().reviewAt.isAfter(LocalDateTime.now())){if(allowAutopilot)autopilot(last.get(),report);return last.get();}
        for(var old:decisions.findByProductIdAndStatus(productId,"PENDING"))old.status="SUPERSEDED";
        decisions.flush();
        var d=new PricingDecision();d.productId=productId;d.createdAt=LocalDateTime.now();d.reviewAt=d.createdAt.plusHours(report.reviewAfterHours());
        d.fingerprint=fingerprint;d.reportJson=json.writeValueAsString(report);decisions.saveAndFlush(d);
        audit.record("SMART_PRICING_RECOMMENDED","product:"+productId,report.action()+" · "+report.suggestedPercent()+"% · уверенность "+report.confidence());if(allowAutopilot)autopilot(d,report);return d;
    }
    private boolean eligible(Product p){return !p.getArchived()&&Set.of(com.aria.enums.ProductStatus.ACTIVE,com.aria.enums.ProductStatus.CANDIDATE,com.aria.enums.ProductStatus.DISCOUNTED).contains(p.getStatus())&&p.getUnitPrice()!=null&&p.getUnitPrice().signum()>0;}
    private Facts facts(Product p){
        var now=LocalDateTime.now();var today=LocalDate.now();var stock=lots.findByProductId(p.getId());
        var usable=stock.stream().filter(inventory::usable).toList();
        var cost=usable.stream().map(InventoryLot::getUnitCost).max(BigDecimal::compareTo).orElse(p.getUnitCost()==null?BigDecimal.ZERO:p.getUnitCost());
        var margin=policies.forProduct(p).getMinimumMarginPercent();
        var minimum=cost.multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(100-margin),2,RoundingMode.CEILING).max(new BigDecimal("0.01"));
        if(p.getMinimumPrice()!=null)minimum=minimum.max(p.getMinimumPrice());
        var rows=usable.stream().sorted(Comparator.comparing(InventoryLot::getId)).map(l->new Lot(l.getId(),l.getQuantity(),l.getUnitCost(),l.getExpiryDate())).toList();
        if(stock.isEmpty()&&inventory.available(p)>0)rows=List.of(new Lot(null,inventory.available(p),cost,p.getExpiryDate()));
        var sales=orders.pricingSales(p.getId(),today.minusDays(29).atStartOfDay(),now).stream().map(r->new Sale((LocalDateTime)r[0],((Number)r[1]).intValue(),(BigDecimal)r[2])).toList();
        int observed=(int)Math.max(1,Math.min(30,ChronoUnit.DAYS.between(p.getReceivedAt(),today)+1));
        int max=pricing.maximum(p);while(max>0&&pricing.price(p,max).compareTo(minimum)<0)max--;
        var history=changes.findByProductIdOrderByChangedAtDescIdDesc(p.getId());var exposures=new ArrayList<PriceExposure>();
        for(int i=0;i<history.size();i++){
            var c=history.get(i);var from=c.changedAt.isAfter(today.minusDays(29).atStartOfDay())?c.changedAt:today.minusDays(29).atStartOfDay();var end=i==0?today.atStartOfDay():history.get(i-1).changedAt;
            if(!from.isBefore(end))continue;
            var price=c.newBasePrice.multiply(BigDecimal.valueOf(100-c.newPercent)).divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP);
            long units=sales.stream().filter(s->!s.shippedAt().isBefore(from)&&s.shippedAt().isBefore(end)&&s.price().compareTo(price)==0).mapToLong(Sale::quantity).sum();
            exposures.add(new PriceExposure(price,Duration.between(from,end).toSeconds()/86400d,units));
        }
        return new Facts(p.getId(),p.getSku(),p.getName(),p.getCategory(),today,p.getUnitPrice(),pricing.price(p,p.getDiscountPercent()),p.getDiscountPercent(),minimum,max,inventory.available(p,stock),observed,rows,sales,exposures,List.of("История изменений цен ведётся с включения Smart Pricing; более ранние акции не восстановлены."),competitors.context(p));
    }
    private String fingerprint(Report report){
        try{var last=changes.findFirstByProductIdOrderByChangedAtDescIdDesc(report.facts().productId()).map(c->c.id).orElse(0L);
            var raw=json.writeValueAsString(report)+"|"+last;
            return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        }catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException("SHA-256 недоступен");}
    }
    @Transactional(readOnly=true) public Map<String,Object> list(int page,int size){
        if(page<0||page>1000000||size<1||size>100)throw new IllegalArgumentException("Неверная страница");
        var result=decisions.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(page,size));
        return Map.of("content",result.getContent().stream().map(this::view).toList(),"totalPages",result.getTotalPages(),"totalElements",result.getTotalElements(),"page",page,"mode",settings.findById(1L).map(s->s.mode).orElse("RECOMMEND"));
    }
    private PricingDecision decision(Long id){return decisions.findById(id).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Решение не найдено"));}
    @Transactional public Map<String,Object> apply(Long id,Integer percent,String reason){
        var d=decision(id);Product p=inventory.lock(d.productId);
        // Refresh decision after product lock: concurrent confirmations must not apply twice.
        refresh(d);if(!"PENDING".equals(d.status))throw new IllegalStateException("Решение уже обработано");
        if(!eligible(p))throw new IllegalStateException("Товар недоступен");
        if(percent==null&&"AI".equals(d.source)&&!Set.of("PRICE","KEEP").contains(d.strategy))throw new IllegalStateException("AI выбрал непрайсовое действие. Изменение цены требует отдельного ручного решения");
        inventory.ensureLegacyLot(p);inventory.synchronize(p);
        var fresh=SmartPricingEngine.evaluate(facts(p));
        if(!d.reviewAt.isAfter(LocalDateTime.now())||!d.fingerprint.equals(fingerprint(fresh)))throw new IllegalStateException("Данные или срок рекомендации изменились. Запустите новый анализ");
        int selected=percent==null?("AI".equals(d.source)&&d.selectedPercent!=null?d.selectedPercent:fresh.suggestedPercent()):percent;
        pricing.validate(p,selected);
        if(pricing.price(p,selected).compareTo(fresh.facts().minimumPrice())<0)throw new IllegalStateException("Цена ниже допустимого минимума");
        control.applyDiscount(p.getId(),new DiscountInput(selected,"Smart Pricing #"+id+": "+reason,pricing.price(p,selected)));
        d.status="APPLIED";d.appliedPercent=selected;d.appliedPrice=pricing.price(p,selected);decide(d,reason);
        d.priceChangeId=changes.findFirstByProductIdOrderByChangedAtDescIdDesc(p.getId()).filter(c->!c.changedAt.isBefore(d.createdAt)).map(c->c.id).orElse(null);
        audit.record("SMART_PRICING_APPLIED","decision:"+id,selected+"% · "+reason);return view(d);
    }
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    private void refresh(PricingDecision d){entityManager.refresh(d);}
    @Transactional public Map<String,Object> reject(Long id,String reason){
        var d=decision(id);inventory.lock(d.productId);refresh(d);
        if(!"PENDING".equals(d.status))throw new IllegalStateException("Решение уже обработано");
        d.status="REJECTED";decide(d,reason);audit.record("SMART_PRICING_REJECTED","decision:"+id,reason);return view(d);
    }
    @Transactional public Map<String,Object> rollback(Long id,String reason){
        var d=decision(id);Product p=inventory.lock(d.productId);refresh(d);
        if(!"APPLIED".equals(d.status)||d.priceChangeId==null)throw new IllegalStateException("Нет изменения для отката");
        var last=changes.findFirstByProductIdOrderByChangedAtDescIdDesc(p.getId()).orElseThrow();
        if(!last.id.equals(d.priceChangeId)||!p.getDiscountPercent().equals(d.appliedPercent)||pricing.price(p,p.getDiscountPercent()).compareTo(d.appliedPrice)!=0)
            throw new IllegalStateException("После решения цена менялась. Откат устаревшего состояния запрещён");
        // Never restore a base price or margin that has since become unsafe.
        inventory.synchronize(p);pricing.validate(p,last.oldPercent);
        control.applyDiscount(p.getId(),new DiscountInput(last.oldPercent,"Откат Smart Pricing #"+id+": "+reason));
        d.status="ROLLED_BACK";decide(d,reason);audit.record("SMART_PRICING_ROLLED_BACK","decision:"+id,reason);return view(d);
    }
    private void decide(PricingDecision d,String reason){d.decidedAt=LocalDateTime.now();var auth=SecurityContextHolder.getContext().getAuthentication();d.decidedBy=auth==null?"system":auth.getName();d.decisionReason=reason;}
    private Map<String,Object> view(PricingDecision d){
        var result=new LinkedHashMap<String,Object>();result.put("id",d.id);result.put("productId",d.productId);result.put("createdAt",d.createdAt);result.put("reviewAt",d.reviewAt);result.put("status",d.status);
        result.put("report",json.readValue(d.reportJson,Map.class));result.put("explanation",d.explanationJson==null?null:json.readValue(d.explanationJson,Map.class));
        result.put("source",d.source);result.put("selectedPercent",d.selectedPercent);result.put("aiRequestedAt",d.aiRequestedAt);result.put("aiCompletedAt",d.aiCompletedAt);
        result.put("strategy",d.strategy);
        result.put("aiLastSkippedAt",d.aiLastSkippedAt);result.put("aiLastSkippedNote",d.aiLastSkippedNote);
        result.put("decidedAt",d.decidedAt);result.put("decidedBy",d.decidedBy);result.put("decisionReason",d.decisionReason);result.put("appliedPercent",d.appliedPercent);result.put("appliedPrice",d.appliedPrice);return result;
    }
    @Transactional(readOnly=true) public Map<String,Object> explanationFacts(Long id){
        var d=decision(id);var report=json.readValue(d.reportJson,Map.class);var p=products.findById(d.productId).orElseThrow();
        var stats=(Map<?,?>)report.get("sales");double rate=((Number)stats.get("unitsPerDay")).doubleValue();int quantity=inventory.available(p);
        double risk=((List<?>)report.get("scenarios")).stream().mapToDouble(s->((Number)((Map<?,?>)s).get("estimatedExpiredUnits")).doubleValue()).min().orElse(0);
        var alternatives=new LinkedHashMap<String,Object>();alternatives.put("socialPossible",risk>0&&quantity>0);alternatives.put("matchingVerifiedOrganizations",charities.matching(p).size());alternatives.put("socialNote","Данные организаций не выдумываются. Регион, пригодность, хранение и транспорт требуют подтверждения. Только черновик без резерва.");
        alternatives.put("reorderBasis",rate>0?"Средняя частота подтверждённых отгрузок; срок поставки и сезонность неизвестны":"Нет подтверждённых продаж для расчёта закупки");
        if(rate>0){alternatives.put("daysOfCoverEstimate",Math.round(quantity/rate*100d)/100d);alternatives.put("unitsFor14DaysEstimate",Math.max(0,(int)Math.ceil(rate*14-quantity)));}
        return Map.of("decisionId",id,"report",report,"alternatives",alternatives);
    }
    @Transactional public Map<String,Object> currentForProposal(Long id){
        var d=decision(id);var p=inventory.lock(d.productId);refresh(d);inventory.synchronize(p);var fresh=SmartPricingEngine.evaluate(facts(p));
        if(!"PENDING".equals(d.status)||!d.reviewAt.isAfter(LocalDateTime.now())||!d.fingerprint.equals(fingerprint(fresh)))throw new IllegalStateException("Сначала создайте актуальную рекомендацию для социальной проверки");return json.readValue(json.writeValueAsString(fresh),Map.class);
    }
    @Transactional public Map<String,Object> saveExplanation(Long id,Map<String,Object> explanation){var d=decision(id);if("AI".equals(explanation.get("mode"))||d.explanationJson==null)d.explanationJson=json.writeValueAsString(explanation);return explanation;}
    @Transactional(readOnly=true) public List<Map<String,Object>> history(Long productId){
        var now=LocalDateTime.now();var history=changes.findByProductIdOrderByChangedAtDescIdDesc(productId);var result=new ArrayList<Map<String,Object>>();
        for(int i=0;i<history.size();i++){
            var c=history.get(i);var end=i==0?now:history.get(i-1).changedAt;end=end.isBefore(c.changedAt.plusDays(7))?end:c.changedAt.plusDays(7);
            var sales=orders.pricingSales(productId,c.changedAt.minusDays(7),end);
            long before=0,after=0;BigDecimal revenue=BigDecimal.ZERO;
            for(var row:sales){var at=(LocalDateTime)row[0];int qty=((Number)row[1]).intValue();if(at.isBefore(c.changedAt))before+=qty;else if(at.isBefore(end)){after+=qty;revenue=revenue.add(((BigDecimal)row[2]).multiply(BigDecimal.valueOf(qty)));}}
            var view=new LinkedHashMap<String,Object>();view.put("change",c);view.put("unitsBefore7Days",before);view.put("unitsAfter",after);view.put("revenueAfter",revenue);view.put("observedUntil",end);
            double elapsed=Math.max(0,Duration.between(c.changedAt,end).toSeconds()/86400d);view.put("daysAfter",Math.round(elapsed*100d)/100d);
            view.put("note","Фактические отгрузки до следующего изменения, максимум 7 дней. Периоды разной длины; причинный эффект скидки не доказан. История наличия и полный учёт затрат отсутствуют.");result.add(view);
        }return result;
    }
    private void autopilot(PricingDecision d,Report report){
        if(!"PENDING".equals(d.status))return;
        // Shared DB lock makes stop/config changes serialize with every automatic price mutation.
        var config=settings.lockSettings().orElse(null);if(config==null||!"AUTOPILOT".equals(config.mode))return;
        if(d.aiClaim!=null)return;
        if(config.externalAiEnabled&&config.scheduledAiEnabled&&d.aiCompletedAt==null)return;
        int target="AI".equals(d.source)&&d.selectedPercent!=null?d.selectedPercent:report.suggestedPercent();
        String action=target>report.facts().currentPercent()?"DISCOUNT":target<report.facts().currentPercent()?"RESTORE":"KEEP";
        String block=null;
        if("AI".equals(d.source)&&!"PRICE".equals(d.strategy))block="AI выбрал непрайсовое действие: нужно подтверждение администратора";
        else if(!json.readValue(config.allowedCategoriesJson,List.class).contains(PolicyService.categoryKey(report.facts().category())))block="Категория не разрешена владельцем";
        else if("AI".equals(d.source)&&("LOW".equals(report.confidence())||d.explanationJson==null||!Boolean.FALSE.equals(json.readValue(d.explanationJson,Map.class).get("requiresConfirmation"))))block="AI требует ручного подтверждения: недостаточная уверенность или сомнения";
        else if(!config.allowLowConfidence&&"LOW".equals(report.confidence()))block="Низкая уверенность: требуется ручное подтверждение";
        else if(target>config.maximumDiscount)block="Рекомендуемая скидка превышает лимит автопилота";
        else if("RESTORE".equals(action)&&!config.allowRestore)block="Повышение цены при уменьшении скидки не разрешено";
        else if(!Set.of("DISCOUNT","RESTORE").contains(action)||Set.of("BLOCKED","REVIEW").contains(report.action()))block="Нет обоснованного изменения цены";
        else if(changes.findFirstByProductIdOrderByChangedAtDescIdDesc(d.productId).filter(c->c.changedAt.plusHours(config.cooldownHours).isAfter(LocalDateTime.now())).isPresent())block="Пауза между изменениями цены ещё не прошла";
        if(block!=null){audit.record("SMART_AUTOPILOT_SKIPPED","decision:"+d.id,block);return;}
        // Uses exactly the same stale-data, expiry, floor and concurrency checks as a human confirmation.
        apply(d.id,target,"AUTOPILOT · "+d.source+": разрешённая категория, предел "+config.maximumDiscount+"%, пауза "+config.cooldownHours+" ч.");
        audit.record("SMART_AUTOPILOT_APPLIED","decision:"+d.id,target+"% · версия настроек "+config.version);
    }
    @Transactional(readOnly=true) public Map<String,Object> settings(){return settingsView(settings.findById(1L).orElseGet(SmartPricingSettings::new));}
    private Map<String,Object> settingsView(SmartPricingSettings s){
        var out=new LinkedHashMap<String,Object>();out.put("aiDailyBudget",s.aiDailyBudget);out.put("aiBudgetUsed",LocalDate.now().equals(s.aiBudgetDate)?s.aiBudgetUsed:0);out.put("version",s.version);out.put("mode",s.mode);out.put("maximumDiscount",s.maximumDiscount);out.put("cooldownHours",s.cooldownHours);
        out.put("publicAssistantAiEnabled",s.publicAssistantAiEnabled);out.put("publicAiDailyBudget",s.publicAiDailyBudget);out.put("publicAiBudgetUsed",LocalDate.now().equals(s.publicAiBudgetDate)?s.publicAiBudgetUsed:0);
        out.put("externalAiEnabled",s.externalAiEnabled);out.put("scheduledAiEnabled",s.scheduledAiEnabled);out.put("allowLowConfidence",s.allowLowConfidence);out.put("allowRestore",s.allowRestore);out.put("allowedCategories",json.readValue(s.allowedCategoriesJson,List.class));out.put("updatedAt",s.updatedAt);return out;
    }
    @Transactional public Map<String,Object> configure(com.aria.controller.SmartPricingController.SettingsInput input){
        var s=settings.lockSettings().orElseGet(SmartPricingSettings::new);
        if(!Objects.equals(s.version,input.version()))throw new IllegalStateException("Настройки уже изменены. Обновите раздел");
        var categories=input.allowedCategories().stream().map(PolicyService::categoryKey).distinct().sorted().toList();
        if("AUTOPILOT".equals(input.mode())&&(categories.isEmpty()||categories.contains("*")))throw new IllegalArgumentException("Для автопилота явно перечислите разрешённые категории, без *");
        boolean external=input.externalAiEnabled()==null?s.externalAiEnabled:input.externalAiEnabled();
        boolean scheduled=input.scheduledAiEnabled()==null?s.scheduledAiEnabled:input.scheduledAiEnabled();
        if(scheduled&&!external)throw new IllegalArgumentException("Для расписания требуется согласие на внешний AI");
        if(input.aiDailyBudget()!=null)s.aiDailyBudget=input.aiDailyBudget()==0?10:input.aiDailyBudget();
        if(input.publicAssistantAiEnabled()!=null)s.publicAssistantAiEnabled=input.publicAssistantAiEnabled();
        if(input.publicAiDailyBudget()!=null)s.publicAiDailyBudget=input.publicAiDailyBudget();
        s.consentRevision++;
        s.externalAiEnabled=external;s.scheduledAiEnabled=scheduled;s.mode=input.mode();s.maximumDiscount=input.maximumDiscount();s.cooldownHours=input.cooldownHours();s.allowLowConfidence=input.allowLowConfidence();s.allowRestore=input.allowRestore();s.allowedCategoriesJson=json.writeValueAsString(categories);s.updatedAt=LocalDateTime.now();settings.saveAndFlush(s);
        String detail=s.mode+" · AI "+s.externalAiEnabled+" · AI-расписание "+s.scheduledAiEnabled+" · AI-лимит "+s.aiDailyBudget+" · максимум "+s.maximumDiscount+"% · низкая уверенность "+s.allowLowConfidence+" · категории "+String.join(", ",categories);
        audit.record("SMART_AUTOPILOT_CONFIGURED","smart-pricing",detail.substring(0,Math.min(1000,detail.length())));return settingsView(s);
    }
    @Transactional public Map<String,Object> stop(){
        var s=settings.lockSettings().orElseGet(SmartPricingSettings::new);s.mode="RECOMMEND";s.scheduledAiEnabled=false;s.consentRevision++;s.updatedAt=LocalDateTime.now();settings.saveAndFlush(s);
        audit.record("SMART_AUTOPILOT_STOPPED","smart-pricing","Экстренное отключение; новые автоматические изменения запрещены. Уже применённые цены можно откатить отдельно.");return settingsView(s);
    }
    public record AiClaim(Long id,String token,Long settingsVersion,Map<String,Object> facts){}
    @Transactional public AiClaim claimAi(Long id,boolean scheduled){
        var d=decision(id);var p=inventory.lock(d.productId);refresh(d);var config=settings.lockSettings().orElseThrow(()->new IllegalStateException("Включите внешний AI в настройках"));
        if(!config.externalAiEnabled||scheduled&&!config.scheduledAiEnabled)throw new IllegalStateException("Нет согласия администратора на этот AI-анализ");
        var now=LocalDateTime.now();
        if(!"PENDING".equals(d.status)||d.aiClaim!=null&&d.aiRequestedAt.isAfter(now.minusSeconds(90)))throw new IllegalStateException("Решение обработано или AI-анализ уже выполняется");
        inventory.synchronize(p);var fresh=SmartPricingEngine.evaluate(facts(p));
        if(fresh.scenarios().isEmpty()||fresh.facts().quantity()==0)throw new IllegalStateException("Нет допустимых сценариев с пригодным остатком для AI");
        if(!d.reviewAt.isAfter(now)||!d.fingerprint.equals(fingerprint(fresh)))throw new IllegalStateException("Данные изменились: сначала пересчитайте рекомендацию");
        if(config.lastAiRequestAt!=null&&config.lastAiRequestAt.isAfter(now.minusMinutes(1)))throw new IllegalStateException("Не чаще одного AI-решения в минуту");
        if(!LocalDate.now().equals(config.aiBudgetDate)){config.aiBudgetDate=LocalDate.now();config.aiBudgetUsed=0;}
        if(config.aiBudgetUsed>=config.aiDailyBudget)throw new IllegalStateException("Достигнут дневной лимит AI-решений");
        config.aiBudgetUsed++;config.lastAiRequestAt=now;settings.saveAndFlush(config);
        d.aiClaim=UUID.randomUUID().toString();d.aiRequestedAt=now;decisions.flush();
        return new AiClaim(d.id,d.aiClaim,config.consentRevision,explanationFacts(d.id));
    }
    @Transactional public Map<String,Object> completeAi(AiClaim claim,Map<String,Object> response,boolean scheduled){
        return completeAi(claim,response,scheduled,true);
    }
    @Transactional public Map<String,Object> completeAiFromManager(String lease,AiClaim claim,Map<String,Object> response,boolean autopilot){
        var state=managerStates.lockState().orElseThrow();
        if(!Objects.equals(state.lease,lease)||"OFF".equals(state.mode)||!state.automaticAiConsent||state.leaseUntil==null||!state.leaseUntil.isAfter(LocalDateTime.now()))throw new IllegalStateException("Цикл менеджера остановлен или его разрешение истекло");
        return completeAi(claim,response,false,autopilot&&"AUTOPILOT".equals(state.mode));
    }
    @Transactional public Map<String,Object> completeAi(AiClaim claim,Map<String,Object> response,boolean scheduled,boolean allowAutopilot){
        var d=decision(claim.id());var p=inventory.lock(d.productId);refresh(d);var config=settings.lockSettings().orElseThrow();
        if(!"PENDING".equals(d.status)||!Objects.equals(d.aiClaim,claim.token())||!Objects.equals(config.consentRevision,claim.settingsVersion())||!config.externalAiEnabled||scheduled&&!config.scheduledAiEnabled)throw new IllegalStateException("AI-ответ устарел или разрешение изменилось");
        inventory.synchronize(p);var report=SmartPricingEngine.evaluate(facts(p));
        if(!d.reviewAt.isAfter(LocalDateTime.now())||!d.fingerprint.equals(fingerprint(report)))throw new IllegalStateException("Данные изменились во время AI-анализа");
        d.source="ENGINE";d.selectedPercent=null;d.strategy="PRICE";
        if("AI".equals(response.get("mode"))){
            Object raw=response.get("suggestedPercent");
            if(!(raw instanceof Number n)||n.doubleValue()!=n.intValue()||report.scenarios().stream().noneMatch(s->s.percent()==n.intValue()))throw new IllegalStateException("AI выбрал недопустимый сценарий");
            if(!(response.get("reviewAfterHours") instanceof Number h)||!List.of(6,12,24).contains(h.intValue())||h.doubleValue()!=h.intValue()||h.intValue()>report.reviewAfterHours())throw new IllegalStateException("Недопустимый срок пересмотра AI");
            pricing.validate(p,n.intValue());d.source="AI";d.selectedPercent=n.intValue();
            String strategy=String.valueOf(response.getOrDefault("strategy","PRICE"));if(!Set.of("PRICE","PROMOTE","SOCIAL_REVIEW","REORDER","KEEP").contains(strategy))throw new IllegalStateException("Недопустимая стратегия AI");d.strategy=strategy;
            if(!"PRICE".equals(strategy)&&n.intValue()!=report.facts().currentPercent())throw new IllegalStateException("Непрайсовая стратегия не может одновременно менять скидку");
            if("SOCIAL_REVIEW".equals(strategy)&&report.scenarios().stream().mapToDouble(s->s.estimatedExpiredUnits()).min().orElse(0)<=0)throw new IllegalStateException("Нет рассчитанного риска списания для социальной стратегии");
            if("REORDER".equals(strategy)&&(report.sales().unitsPerDay()<=0||report.sales().unitsPerDay()*14<=report.facts().quantity()))throw new IllegalStateException("Нет данных для рекомендации закупки");
            var proposed=LocalDateTime.now().plusHours(h.intValue());if(proposed.isBefore(d.reviewAt))d.reviewAt=proposed;
        }
        d.explanationJson=json.writeValueAsString(response);d.aiCompletedAt=LocalDateTime.now();d.aiClaim=null;d.aiLastSkippedAt=null;d.aiLastSkippedNote=null;
        audit.record("SMART_AI_DECISION","decision:"+d.id,d.source+" · "+(d.selectedPercent==null?report.suggestedPercent():d.selectedPercent)+"% · исходные ограничения повторно проверены");
        decisions.saveAndFlush(d); // apply() refreshes the row; keep the validated AI selection in the same atomic transaction.
        if(allowAutopilot)autopilot(d,report);return view(d);
    }
    @Transactional public void recordSkippedAi(Long id,String reason){
        var d=decision(id);inventory.lock(d.productId);refresh(d);var config=settings.lockSettings().orElse(null);
        if(config==null||!config.externalAiEnabled||!config.scheduledAiEnabled||!"PENDING".equals(d.status)||d.aiClaim!=null&&d.aiRequestedAt.isAfter(LocalDateTime.now().minusSeconds(90)))return;
        d.aiLastSkippedAt=LocalDateTime.now();d.aiLastSkippedNote=reason.substring(0,Math.min(reason.length(),1000));
        audit.record("SMART_AI_SKIPPED","decision:"+id,d.aiLastSkippedNote);
    }
}
