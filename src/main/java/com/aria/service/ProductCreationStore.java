package com.aria.service;

import com.aria.entity.*;
import com.aria.repository.*;
import com.aria.dto.ControlRequests.ProductInput;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

/** Short committed transactions surround remote inference. Settings lock serializes quota and consent. */
@Service @Transactional
public class ProductCreationStore {
    private static final ZoneId ZONE=ZoneId.of("Asia/Qyzylorda");
    private final ProductCreationSettingsRepository settings;
    private final ProductAiDraftRepository drafts;
    private final AuditService audit;
    private final ControlService control;
    private final tools.jackson.databind.ObjectMapper json=tools.jackson.databind.json.JsonMapper.builder().build();
    public ProductCreationStore(ProductCreationSettingsRepository settings,ProductAiDraftRepository drafts,AuditService audit,ControlService control){this.settings=settings;this.drafts=drafts;this.audit=audit;this.control=control;}
    private ProductCreationSettings lock(){return settings.lockSettings().orElseGet(()->settings.saveAndFlush(new ProductCreationSettings()));}
    public Map<String,Object> configuration(){return settingsView(lock());}
    public Map<String,Object> configure(long revision,boolean enabled,int budget){
        var s=lock();if(s.version!=revision)throw new IllegalStateException("Настройки уже изменены. Обновите форму.");
        if(budget<1||budget>20)throw new IllegalArgumentException("Лимит должен быть от 1 до 20 попыток в день.");
        s.textConsentEnabled=enabled;s.dailyBudget=budget;s.consentRevision++;s.updatedAt=LocalDateTime.now(ZONE);settings.saveAndFlush(s);
        audit.record(enabled?"PRODUCT_AI_TEXT_CONSENT_GRANTED":"PRODUCT_AI_TEXT_CONSENT_REVOKED","product-creation","Только введённое название товара в OpenRouter; фото, контакты, себестоимость и склад не передаются. Лимит "+budget+" попыток/день.");
        return settingsView(s);
    }
    private Map<String,Object> settingsView(ProductCreationSettings s){
        int used=LocalDate.now(ZONE).equals(s.budgetDate)?s.budgetUsed:0;
        return Map.of("revision",s.version,"textConsentEnabled",s.textConsentEnabled,"dailyBudget",s.dailyBudget,"budgetUsed",used,"remaining",Math.max(0,s.dailyBudget-used),"updatedAt",s.updatedAt,"timeZone",ZONE.toString());
    }
    public record Claim(Long id,long consentRevision,String name){}
    public Claim claim(String name){
        var s=lock();if(!s.textConsentEnabled)throw new IllegalStateException("Разрешите создание текста в настройках AI для товаров.");
        var now=LocalDateTime.now(ZONE);
        if(s.lastRequestAt!=null&&s.lastRequestAt.isAfter(now.minusSeconds(60)))throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Создавать текст можно не чаще одного раза в минуту.");
        if(!now.toLocalDate().equals(s.budgetDate)){s.budgetDate=now.toLocalDate();s.budgetUsed=0;}
        if(s.budgetUsed>=s.dailyBudget)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Дневной лимит создания текста исчерпан. Товар можно добавить вручную.");
        s.lastRequestAt=now;s.budgetUsed++;settings.saveAndFlush(s);
        var d=new ProductAiDraft();d.inputName=name;d.consentRevision=s.consentRevision;drafts.saveAndFlush(d);
        audit.record("PRODUCT_AI_DRAFT_REQUESTED","product-ai-draft:"+d.id,"Запрошен текстовый черновик без отправки фото и данных магазина.");return new Claim(d.id,s.consentRevision,name);
    }
    public Map<String,Object> complete(Claim claim,ProductDraftModelService.Output output){
        var s=lock();var d=drafts.lockById(claim.id()).orElseThrow(()->new IllegalStateException("Черновик не найден."));
        if(!"REQUESTED".equals(d.status))throw new IllegalStateException("Черновик уже обработан.");
        if(!s.textConsentEnabled||s.consentRevision!=claim.consentRevision()||d.consentRevision!=claim.consentRevision()){
            d.status="CANCELLED";d.completedAt=LocalDateTime.now(ZONE);d.error="Согласие изменилось во время запроса. Ответ не сохранён.";
            audit.record("PRODUCT_AI_DRAFT_CANCELLED","product-ai-draft:"+d.id,d.error);return view(d);
        }
        d.status="DRAFT";d.name=output.name();d.category=output.category();d.description=output.description();d.questionsJson=json.writeValueAsString(output.questions());d.model=output.model();d.completedAt=LocalDateTime.now(ZONE);
        audit.record("PRODUCT_AI_DRAFT_CREATED","product-ai-draft:"+d.id,"Получен проверенный ответ модели. Текст не подтверждён владельцем и не является проверкой характеристик.");return view(d);
    }
    public void fail(Claim claim,String error){
        var s=lock();var d=drafts.lockById(claim.id()).orElseThrow();if(!"REQUESTED".equals(d.status))return;
        d.status=s.textConsentEnabled&&s.consentRevision==claim.consentRevision()?"FAILED":"CANCELLED";d.completedAt=LocalDateTime.now(ZONE);d.error=error;
        audit.record("PRODUCT_AI_DRAFT_FAILED","product-ai-draft:"+d.id,error);
    }
    public Map<String,Object> draft(Long id){return view(drafts.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Черновик не найден.")));}
    public Product confirm(Long id,boolean confirmed,ProductInput input){
        if(!confirmed)throw new IllegalArgumentException("Подтвердите проверку предложенного текста перед созданием товара.");
        var s=lock();var d=drafts.lockById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Черновик не найден."));
        if(!"DRAFT".equals(d.status)||d.productId!=null)throw new IllegalStateException("Черновик уже использован или недоступен. Новый товар не создан.");
        if(!s.textConsentEnabled||d.consentRevision!=s.consentRevision)throw new IllegalStateException("Согласие изменилось. Создайте новый черновик или добавьте товар вручную.");
        if(d.createdAt.isBefore(LocalDateTime.now(ZONE).minusDays(7)))throw new IllegalStateException("Черновик старше 7 дней. Создайте новый или добавьте товар вручную.");
        try{ProductDraftModelService.validate(d.inputName,new ProductDraftModelService.Output(d.name,d.category,d.description,d.questionsJson==null?List.of():json.readValue(d.questionsJson,List.class),d.model));}
        catch(ProductDraftModelService.Unavailable invalid){throw new IllegalStateException("Этот черновик не отвечает действующим правилам безопасного текста. Создайте новый или добавьте товар вручную. Исходный ответ сохранён в истории.");}
        if(input.sku()!=null&&!input.sku().isBlank())throw new IllegalArgumentException("Код нового товара создаётся автоматически.");
        var product=control.saveProduct(null,input);d.status="CONFIRMED";d.productId=product.getId();d.confirmedAt=LocalDateTime.now(ZONE);
        audit.record("PRODUCT_AI_TEXT_CONFIRMED","product:"+product.getId(),"Владелец проверил текст черновика "+d.id+". Характеристики, изготовитель и пригодность партии этим не подтверждаются.");return product;
    }
    public Map<String,Object> provenance(Long productId){
        var draft=drafts.findByProductId(productId);return draft.isPresent()?view(draft.get()):Map.of("source","OWNER","verificationStatus","NOT_AI_GENERATED");
    }
    private Map<String,Object> view(ProductAiDraft d){
        var out=new LinkedHashMap<String,Object>();out.put("id",d.id);out.put("status",d.status);out.put("source",d.source);out.put("name",d.name);out.put("category",d.category);out.put("description",d.description);out.put("questions",d.questionsJson==null?List.of():json.readValue(d.questionsJson,List.class));out.put("model",d.model);out.put("createdAt",d.createdAt);out.put("completedAt",d.completedAt);out.put("confirmedAt",d.confirmedAt);out.put("productId",d.productId);out.put("error",d.error);out.put("verificationStatus","CONFIRMED".equals(d.status)?"OWNER_REVIEWED_TEXT":"UNCONFIRMED_TEXT");out.put("note","Предложенный текст. Проверка владельцем не подтверждает характеристики производителя или срок партии.");return out;
    }
}
