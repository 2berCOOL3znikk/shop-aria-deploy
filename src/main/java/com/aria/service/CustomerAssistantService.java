package com.aria.service;
import com.aria.repository.SmartPricingSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
public class CustomerAssistantService {
    private final CustomerCatalogueService catalogue;private final IntelligenceService ai;private final SmartPricingSettingsRepository settings;private final TransactionTemplate transaction;
    public CustomerAssistantService(CustomerCatalogueService catalogue,IntelligenceService ai,SmartPricingSettingsRepository settings,PlatformTransactionManager manager){this.catalogue=catalogue;this.ai=ai;this.settings=settings;transaction=new TransactionTemplate(manager);}
    public Map<String,Object> status(){return Map.of("externalAiAvailable",settings.findById(1L).filter(s->s.publicAssistantAiEnabled).isPresent()&&Boolean.TRUE.equals(ai.pricingStatus().get("configured")),"note","Внешний AI получает только ваше сообщение и публичные карточки после вашего согласия. Поиск по каталогу работает и без внешнего AI.");}
    public Map<String,Object> answer(String query,List<Long> compared,boolean consent){
        var parsed=AssistantQuery.parse(query);var all=catalogue.available();
        if(!compared.isEmpty()&&all.stream().filter(p->compared.contains(((Number)p.get("id")).longValue())).count()!=new HashSet<>(compared).size())throw new IllegalArgumentException("Один из выбранных товаров недоступен в каталоге");
        var candidates=all.stream().filter(p->compared.isEmpty()||compared.contains(((Number)p.get("id")).longValue()))
            .filter(p->parsed.budget()==null||new BigDecimal(p.get("displayPrice").toString()).compareTo(parsed.budget())<=0)
            .filter(p->!parsed.discountsOnly()||((Number)p.get("discountPercent")).intValue()>0)
            .filter(p->!parsed.withoutDiscounts()||((Number)p.get("discountPercent")).intValue()==0)
            .sorted(Comparator.<Map<String,Object>>comparingInt(p->score(parsed,p)).reversed().thenComparing(p->new BigDecimal(p.get("displayPrice").toString())).thenComparing(p->((Number)p.get("id")).longValue())).limit(40).toList();
        if("ALTERNATIVES".equals(parsed.intent())){var seed=all.stream().filter(p->score(parsed,p)>0).max(Comparator.comparingInt(p->score(parsed,p)));if(seed.isPresent()){var original=seed.get();candidates=candidates.stream().filter(p->Objects.equals(p.get("category"),original.get("category"))&&!Objects.equals(p.get("id"),original.get("id"))).toList();}}
        String source="ENGINE",note="Подбор по текущему каталогу и заданным ограничениям.";List<Long> chosen=candidates.stream().filter(p->parsed.terms().isEmpty()||score(parsed,p)>0||!compared.isEmpty()||"ALTERNATIVES".equals(parsed.intent())).limit(5).map(p->((Number)p.get("id")).longValue()).toList();
        if(consent&&!candidates.isEmpty()&&claimPublicBudget()){
            var result=ai.choosePublicProducts(query,candidates);if("AI".equals(result.get("mode"))){source="AI";chosen=((List<?>)result.get("productIds")).stream().map(v->((Number)v).longValue()).toList();note="Модель выбрала товары; цены и доступность проверены сервером по каталогу.";}else note="Внешний AI недоступен. Показан подбор по текущему каталогу.";
        }else if(consent&&!candidates.isEmpty())note="Внешний AI выключен или достигнут его лимит. Показан подбор по текущему каталогу.";
        // Network ran without a DB transaction; refresh availability and prices before returning.
        var ids=chosen;var fresh=catalogue.available().stream().filter(p->ids.contains(((Number)p.get("id")).longValue())).filter(p->parsed.budget()==null||new BigDecimal(p.get("displayPrice").toString()).compareTo(parsed.budget())<=0)
            .filter(p->!parsed.discountsOnly()||((Number)p.get("discountPercent")).intValue()>0).filter(p->!parsed.withoutDiscounts()||((Number)p.get("discountPercent")).intValue()==0)
            .sorted(Comparator.comparingInt(p->ids.indexOf(((Number)p.get("id")).longValue()))).toList();
        String answer=fresh.isEmpty()?"Подходящих доступных товаров не найдено. Попробуйте изменить бюджет или запрос.":"COMPARE".equals(parsed.intent())?"Сравните актуальные цены, категории, единицы и характеристики выбранных товаров ниже. Упаковки могут отличаться.":"Подходящие доступные товары показаны ниже. Цена и наличие повторно проверяются при оформлении.";
        return Map.of("source",source,"intent",parsed.intent(),"text",answer,"note",note,"products",fresh);
    }
    private int score(AssistantQuery q,Map<String,Object> p){String text=(p.get("name")+" "+p.get("category")+" "+p.getOrDefault("brand","")+" "+p.get("sku")).toLowerCase(Locale.ROOT).replace('ё','е');return (int)q.terms().stream().filter(term->text.contains(term)||term.startsWith("продукт")&&text.contains("продукт")).count();}
    private boolean claimPublicBudget(){return Boolean.TRUE.equals(transaction.execute(tx->{var config=settings.lockSettings().orElse(null);if(config==null||!config.publicAssistantAiEnabled||!Boolean.TRUE.equals(ai.pricingStatus().get("configured")))return false;
        var now=LocalDateTime.now();if(config.publicAiLastRequestAt!=null&&config.publicAiLastRequestAt.isAfter(now.minusMinutes(1)))return false;
        if(!LocalDate.now().equals(config.publicAiBudgetDate)){config.publicAiBudgetDate=LocalDate.now();config.publicAiBudgetUsed=0;}
        if(config.publicAiBudgetUsed>=config.publicAiDailyBudget)return false;config.publicAiBudgetUsed++;config.publicAiLastRequestAt=now;settings.saveAndFlush(config);return true;}));}
}
