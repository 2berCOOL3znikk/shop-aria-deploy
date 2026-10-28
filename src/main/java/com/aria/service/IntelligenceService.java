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
    private final String key,model;
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final tools.jackson.databind.ObjectMapper json=tools.jackson.databind.json.JsonMapper.builder().build();
    public IntelligenceService(@Value("${ARIA_OPENROUTER_API_KEY:}")String key,@Value("${ARIA_OPENROUTER_MODEL:openai/gpt-4o-mini}")String model){this.key=key;this.model=model;}
    public Map<String,Object> explain(Map<String,Object> facts){
        String fallback=String.join("\n",((List<?>)facts.get("briefing")).stream().map(Object::toString).toList());
        if(key.isBlank())return Map.of("mode","RULES","configured",false,"text",fallback,"note","Сводка рассчитана Java. Для AI-объяснения задайте серверную переменную ARIA_OPENROUTER_API_KEY.");
        try {
            String body=json.writeValueAsString(Map.of("model",model,"temperature",0.2,"max_tokens",700,"messages",List.of(
                Map.of("role","system","content","Ты помощник руководителя ARIA. По-русски кратко объясни только переданные агрегированные факты. Не выдумывай цены, остатки, прогнозы и фактические передачи. Не выполняй операции. Дай 3 конкретных приоритета и укажи ограничения данных."),
                Map.of("role","user","content",json.writeValueAsString(facts)))));
            HttpRequest req=HttpRequest.newBuilder(URI.create("https://openrouter.ai/api/v1/chat/completions")).timeout(Duration.ofSeconds(25)).header("Authorization","Bearer "+key).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response=client.send(req,HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()!=200)throw new IllegalStateException("Provider unavailable");
            Map<?,?> parsed=json.readValue(response.body(),Map.class);
            var choices=(List<?>)parsed.get("choices");var choice=(Map<?,?>)choices.getFirst();var message=(Map<?,?>)choice.get("message");String text=(String)message.get("content");
            if(text==null||text.isBlank())throw new IllegalStateException("Empty response");
            return Map.of("mode","AI","configured",true,"text",text,"note","Объяснение OpenRouter по агрегированным данным. Решения подтверждает сотрудник.");
        }catch(InterruptedException e){Thread.currentThread().interrupt();return Map.of("mode","RULES","configured",true,"text",fallback,"note","AI-запрос прерван. Показана сводка правил.");}
        catch(Exception e){return Map.of("mode","RULES","configured",true,"text",fallback,"note","AI временно недоступен. Показана сводка правил без потери функций магазина.");}
    }
}
