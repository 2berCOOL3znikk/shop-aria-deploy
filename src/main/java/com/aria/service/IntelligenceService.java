package com.aria.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

/** External model can explain aggregate facts, but has no write tools or authority over stock/prices. */
@Service
public class IntelligenceService {
    @Value("${aria.intelligence.external-enabled:true}")
    private boolean externalEnabled=true;
    @Value("${aria.intelligence.free-only:true}")
    private boolean freeOnly=true;
    private final String key,model;
    private final HttpClient client;
    private final tools.jackson.databind.ObjectMapper json=tools.jackson.databind.json.JsonMapper.builder().build();
    @org.springframework.beans.factory.annotation.Autowired
    public IntelligenceService(@Value("${ARIA_OPENROUTER_API_KEY:}")String key,@Value("${ARIA_OPENROUTER_MODEL:openrouter/free}")String model){this(key,model,HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build());}
    IntelligenceService(String key,String model,HttpClient client){this.key=key.trim();this.model=model.trim();this.client=client;}
    public Map<String,Object> explain(Map<String,Object> facts){
        String fallback=String.join("\n",((List<?>)facts.get("briefing")).stream().map(Object::toString).toList());
        if(!externalEnabled)return Map.of("mode","RULES","configured",false,"text",fallback,"note","Сводка рассчитана по данным магазина. Внешний AI отключён настройками.");
        if(key.isBlank())return Map.of("mode","RULES","configured",false,"text",fallback,"note","Сводка рассчитана Java. Для AI-объяснения задайте серверную переменную ARIA_OPENROUTER_API_KEY.");
        if(freeOnly && !(model.equals("openrouter/free")||model.endsWith(":free")))return Map.of("mode","RULES","configured",true,"text",fallback,"note","Выбранная модель не является бесплатной. Запрос заблокирован, показана сводка правил.");
        try {
            Map<String,Object> requestBody=new HashMap<>(Map.of("model",model,"temperature",0.2,"max_tokens",700,"messages",List.of(
                Map.of("role","system","content","Ты помощник руководителя ARIA. По-русски кратко объясни только переданные агрегированные факты. Не выдумывай цены, остатки, прогнозы и фактические передачи. Не выполняй операции. Дай 3 конкретных приоритета и укажи ограничения данных."),
                Map.of("role","user","content",json.writeValueAsString(facts)))));
            if(freeOnly)requestBody.put("provider",Map.of("max_price",Map.of("prompt",0,"completion",0,"request",0,"image",0)));
            requestBody.put("reasoning",Map.of("enabled",false,"exclude",true));
            String body=json.writeValueAsString(requestBody);
            HttpRequest req=HttpRequest.newBuilder(URI.create("https://openrouter.ai/api/v1/chat/completions")).timeout(Duration.ofSeconds(25)).header("Authorization","Bearer "+key).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response=client.send(req,HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()==429)return Map.of("mode","RULES","configured",true,"text",fallback,"note","Лимит бесплатного AI временно исчерпан. Показана сводка правил; платная модель не используется.");
            if(response.statusCode()==401||response.statusCode()==403)return Map.of("mode","RULES","configured",true,"text",fallback,"note","OpenRouter отклонил ключ или доступ к модели. Проверьте ключ в настройках сервера. Показана сводка правил.");
            if(response.statusCode()!=200)throw new IllegalStateException("Provider unavailable");
            Map<?,?> parsed=json.readValue(response.body(),Map.class);
            var choices=(List<?>)parsed.get("choices");var choice=(Map<?,?>)choices.getFirst();var message=(Map<?,?>)choice.get("message");String text=(String)message.get("content");
            if(text==null||text.isBlank())throw new IllegalStateException("Empty response");
            String actualModel=parsed.get("model") instanceof String value?value:model;
            return Map.of("mode","AI","configured",true,"model",actualModel,"text",text,"note","Объяснение OpenRouter · "+actualModel+" · по агрегированным данным. Решения подтверждает сотрудник.");
        }catch(InterruptedException e){Thread.currentThread().interrupt();return Map.of("mode","RULES","configured",true,"text",fallback,"note","AI-запрос прерван. Показана сводка правил.");}
        catch(Exception e){return Map.of("mode","RULES","configured",true,"text",fallback,"note","AI временно недоступен. Показана сводка правил без потери функций магазина.");}
    }
}
