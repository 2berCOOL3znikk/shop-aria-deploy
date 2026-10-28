package com.aria.service;

import org.junit.jupiter.api.Test;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.Flow;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class IntelligenceServiceTest {
    private final Map<String,Object> facts=Map.of("briefing",List.of("Просроченных партий: 0"),"products",3);
    @Test void paidModelIsBlockedBeforeNetworkAccess(){
        var client=mock(HttpClient.class);var service=new IntelligenceService("test-key","openai/gpt-4o-mini",client);
        assertThat(service.explain(facts)).containsEntry("mode","RULES");verifyNoInteractions(client);
    }
    @Test void noKeyDoesNotSendAnyNetworkRequest(){
        var client=mock(HttpClient.class);assertThat(new IntelligenceService("","openrouter/free",client).explain(facts)).containsEntry("configured",false);verifyNoInteractions(client);
    }
    @Test void freeRouterUsesZeroPriceLimitsAndNoPaidFallback()throws Exception {
        var client=mock(HttpClient.class);HttpResponse<String> response=mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);when(response.body()).thenReturn("{\"model\":\"test/model:free\",\"choices\":[{\"message\":{\"content\":\"Проверьте сроки партий\"}}]}");
        when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(response);
        var result=new IntelligenceService("test-key","openrouter/free",client).explain(facts);
        assertThat(result).containsEntry("mode","AI").containsEntry("model","test/model:free");
        var captor=org.mockito.ArgumentCaptor.forClass(HttpRequest.class);verify(client).send(captor.capture(),any(HttpResponse.BodyHandler.class));
        var request=captor.getValue();assertThat(request.headers().firstValue("Authorization")).contains("Bearer test-key");
        String body=body(request);var json=tools.jackson.databind.json.JsonMapper.builder().build().readValue(body,Map.class);
        assertThat(json).containsEntry("model","openrouter/free").doesNotContainKey("models");
        Map<?,?> prices=(Map<?,?>)((Map<?,?>)json.get("provider")).get("max_price");
        assertThat(prices.values()).allMatch(value->((Number)value).intValue()==0);
    }
    @Test void rateLimitKeepsLocalFactsAndNeverRetriesPaidModels()throws Exception {
        var client=mock(HttpClient.class);HttpResponse<String> response=mock(HttpResponse.class);when(response.statusCode()).thenReturn(429);
        when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(response);
        var result=new IntelligenceService("test-key","openrouter/free",client).explain(facts);
        assertThat(result).containsEntry("mode","RULES").containsEntry("text","Просроченных партий: 0");
        verify(client,times(1)).send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
    }
    private Map<String,Object> pricingFacts(){return Map.of("decisionId",1,"report",Map.of("scenarios",List.of(Map.of("percent",0),Map.of("percent",5)),"confidence","LOW"));}
    private HttpClient pricingClient(String content)throws Exception {
        var client=mock(HttpClient.class);HttpResponse<String> response=mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        var json=tools.jackson.databind.json.JsonMapper.builder().build();
        when(response.body()).thenReturn(json.writeValueAsString(Map.of("model","test/model:free","choices",List.of(Map.of("message",Map.of("content",content))))));
        when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(response);return client;
    }
    @Test void pricingJsonIsValidatedAndNeverSentToMutationTools()throws Exception {
        var client=pricingClient("{\"explanation\":\"Истории мало\",\"risks\":[\"Неизвестна реакция\"],\"suggestedPercent\":5}");
        var service=new IntelligenceService("test-key","openrouter/free",client);
        assertThat(service.explainPricing(pricingFacts())).containsEntry("mode","AI").containsEntry("suggestedPercent",5);
        var captor=org.mockito.ArgumentCaptor.forClass(HttpRequest.class);verify(client).send(captor.capture(),any(HttpResponse.BodyHandler.class));
        var request=tools.jackson.databind.json.JsonMapper.builder().build().readValue(body(captor.getValue()),Map.class);
        assertThat(request).doesNotContainKeys("tools","models");assertThat(((Map<?,?>)request.get("response_format")).get("type")).isEqualTo("json_schema");
        assertThat(((Map<?,?>)request.get("provider")).get("require_parameters")).isEqualTo(true);
        assertThat(((Map<?,?>)((Map<?,?>)request.get("provider")).get("max_price")).values()).allMatch(v->((Number)v).intValue()==0);
        assertThat(service.explainPricing(pricingFacts())).containsEntry("mode","ENGINE");verify(client,times(1)).send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
    }
    @Test void pricingRejectsHallucinatedPercentAndFractionalPercent()throws Exception {
        for(String percent:List.of("80","5.5","-1","null")){
            var client=pricingClient("{\"explanation\":\"Пример\",\"risks\":[],\"suggestedPercent\":"+percent+"}");
            assertThat(new IntelligenceService("test-key","openrouter/free",client).explainPricing(pricingFacts())).containsEntry("mode","ENGINE");
        }
    }
    @Test void pricingRejectsInvalidJsonExtraFieldsAndMalformedRisks()throws Exception {
        for(String content:List.of("not json","{\"explanation\":\"Пример\",\"risks\":[],\"suggestedPercent\":5,\"execute\":true}","{\"explanation\":\"Пример\",\"risks\":[1],\"suggestedPercent\":5}")){
            var client=pricingClient(content);assertThat(new IntelligenceService("test-key","openrouter/free",client).explainPricing(pricingFacts())).containsEntry("mode","ENGINE");
        }
    }
    @Test void pricingNoKeyOrPaidModelNeverContactsProvider(){
        var client=mock(HttpClient.class);assertThat(new IntelligenceService("","openrouter/free",client).explainPricing(pricingFacts())).containsEntry("mode","ENGINE");
        assertThat(new IntelligenceService("test-key","paid/model",client).explainPricing(pricingFacts())).containsEntry("mode","ENGINE");verifyNoInteractions(client);
    }
    @Test void pricingTimeoutIsNotMisreportedAsInvalidJson()throws Exception {
        var client=mock(HttpClient.class);when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenThrow(new HttpTimeoutException("timeout"));
        var result=new IntelligenceService("test-key","openrouter/free",client).explainPricing(pricingFacts());assertThat(result).containsEntry("mode","ENGINE");assertThat((String)result.get("note")).contains("25 секунд").doesNotContain("структуры");
    }
    @Test void activeDecisionRequiresBoundedReviewAndConfirmationFlag()throws Exception {
        var facts=Map.<String,Object>of("report",Map.of("scenarios",List.of(Map.of("percent",0),Map.of("percent",5)),"reviewAfterHours",12));
        var good=pricingClient("{\"explanation\":\"Проверить эксперимент\",\"risks\":[],\"suggestedPercent\":5,\"reviewAfterHours\":6,\"requiresConfirmation\":true}");
        assertThat(new IntelligenceService("test-key","openrouter/free",good).decidePricing(facts)).containsEntry("mode","AI").containsEntry("suggestedPercent",5);
        var invalid=pricingClient("{\"explanation\":\"Слишком поздно\",\"risks\":[],\"suggestedPercent\":5,\"reviewAfterHours\":24,\"requiresConfirmation\":false}");
        assertThat(new IntelligenceService("test-key","openrouter/free",invalid).decidePricing(facts)).containsEntry("mode","ENGINE");
    }
    @Test void publicAssistantCanOnlyChooseRealIdsAndNeverSendsPurchaseCosts()throws Exception {
        var client=pricingClient("{\"productIds\":[1]}");var row=Map.<String,Object>of("id",1,"name","Шоколад","displayPrice",1000,"unitCost",800,"ariaReason","private-internal-reason");
        var result=new IntelligenceService("test-key","openrouter/free",client).choosePublicProducts("Шоколад до 2000",List.of(row));assertThat(result).containsEntry("mode","AI");
        var captor=org.mockito.ArgumentCaptor.forClass(HttpRequest.class);verify(client).send(captor.capture(),any(HttpResponse.BodyHandler.class));assertThat(body(captor.getValue())).doesNotContain("unitCost","ariaReason","private-internal-reason");
        var invalid=pricingClient("{\"productIds\":[999]}");assertThat(new IntelligenceService("test-key","openrouter/free",invalid).choosePublicProducts("Товар",List.of(row))).containsEntry("mode","ENGINE");
        var forged=pricingClient("{\"productIds\":[1],\"price\":1}");assertThat(new IntelligenceService("test-key","openrouter/free",forged).choosePublicProducts("Товар",List.of(row))).containsEntry("mode","ENGINE");
    }
    private String body(HttpRequest request)throws Exception {
        var done=new CompletableFuture<String>();var out=new java.io.ByteArrayOutputStream();
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>(){
            public void onSubscribe(Flow.Subscription subscription){subscription.request(Long.MAX_VALUE);}
            public void onNext(ByteBuffer buffer){byte[] bytes=new byte[buffer.remaining()];buffer.get(bytes);out.writeBytes(bytes);}
            public void onError(Throwable exception){done.completeExceptionally(exception);}
            public void onComplete(){done.complete(out.toString(StandardCharsets.UTF_8));}
        });return done.get(2,TimeUnit.SECONDS);
    }
}
