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
