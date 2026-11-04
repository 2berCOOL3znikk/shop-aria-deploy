package com.aria.service;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

/** Intentionally non-transactional: network inference never holds database locks. */
@Service
public class ProductCreationService {
    private final ProductCreationStore store;private final ProductDraftModelService ai;
    public ProductCreationService(ProductCreationStore store,ProductDraftModelService ai){this.store=store;this.ai=ai;}
    public Map<String,Object> settings(){var out=new LinkedHashMap<>(store.configuration());out.putAll(ai.status());return out;}
    public Map<String,Object> generate(String name){
        if(name==null||name.isBlank()||name.length()>300)throw new IllegalArgumentException("Укажите название товара до 300 символов.");
        if(!Boolean.TRUE.equals(ai.status().get("configured")))throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"ИИ для текста не настроен. Товар можно добавить вручную.");
        var claim=store.claim(name.trim());
        ProductDraftModelService.Output result;
        try{result=ai.generate(claim.name());ProductDraftModelService.validate(claim.name(),result);}catch(ProductDraftModelService.Unavailable e){store.fail(claim,e.getMessage());throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,e.getMessage());}
        var saved=store.complete(claim,result);if(!"DRAFT".equals(saved.get("status")))throw new IllegalStateException("Согласие изменилось во время запроса. Ответ ИИ не сохранён.");return saved;
    }
}
