package com.aria.service;

import org.junit.jupiter.api.Test;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ProductDraftModelServiceTest {
    private final tools.jackson.databind.ObjectMapper json=tools.jackson.databind.json.JsonMapper.builder().build();
    private static final String VALID="{\"name\":\"Шоколад\",\"category\":\"Продукты\",\"description\":\"Подробные характеристики товара уточняются.\",\"questions\":[\"Какой состав указан на упаковке?\"]}";
    private HttpClient response(String content,int status)throws Exception{
        var client=mock(HttpClient.class);HttpResponse<byte[]> response=mock(HttpResponse.class);when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(json.writeValueAsBytes(Map.of("model","test/model:free","choices",List.of(Map.of("finish_reason","stop","message",Map.of("content",content))))));
        when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(response);return client;
    }
    @Test void noKeyAndPaidModelNeverContactNetwork(){
        var client=mock(HttpClient.class);
        for(var ai:List.of(new ProductDraftModelService("","openrouter/free",client),new ProductDraftModelService("test-key","paid/model",client))){
            assertThat(ai.status()).containsEntry("configured",false);assertThatThrownBy(()->ai.generate("Товар")).isInstanceOf(ProductDraftModelService.Unavailable.class);
        }verifyNoInteractions(client);
    }
    @Test void strictTextOnlyRequestRequiresFreeProviderAndDoesNotIncludeStoreDataOrTools()throws Exception{
        var client=response(VALID,200);var result=new ProductDraftModelService("test-key","openrouter/free",client).generate("Шоколад");assertThat(result.name()).isEqualTo("Шоколад");assertThat(result.model()).isEqualTo("test/model:free");
        var captor=org.mockito.ArgumentCaptor.forClass(HttpRequest.class);verify(client).send(captor.capture(),any(HttpResponse.BodyHandler.class));var request=captor.getValue();
        assertThat(request.uri().toString()).isEqualTo("https://openrouter.ai/api/v1/chat/completions");assertThat(request.timeout()).contains(java.time.Duration.ofSeconds(25));
        var body=json.readValue(body(request),Map.class);assertThat(body).doesNotContainKeys("tools","models","image_url","catalog");
        assertThat(((Map<?,?>)((Map<?,?>)body.get("provider")).get("max_price")).values()).allMatch(n->((Number)n).intValue()==0);
        var format=(Map<?,?>)body.get("response_format");var schema=(Map<?,?>)((Map<?,?>)format.get("json_schema")).get("schema");assertThat(schema.get("additionalProperties")).isEqualTo(false);
        assertThat(((Map<String,Object>)schema.get("properties")).keySet()).containsExactlyInAnyOrder("name","category","description","questions");
        var properties=(Map<?,?>)schema.get("properties");assertThat(((Map<?,?>)properties.get("name")).get("enum")).isEqualTo(List.of("Шоколад"));assertThat(((Map<?,?>)properties.get("description")).get("enum")).isEqualTo(ProductDraftModelService.DESCRIPTIONS);
        assertThat(((Map<?,?>)properties.get("questions")).containsKey("uniqueItems")).isFalse();
        var messages=(List<Map<String,Object>>)body.get("messages");var input=json.readValue((String)messages.get(1).get("content"),Map.class);assertThat(input).containsOnlyKeys("ownerProvidedName","allowedNames","allowedCategories","allowedDescriptions","allowedQuestions");assertThat(input.get("allowedDescriptions")).isEqualTo(ProductDraftModelService.DESCRIPTIONS);
    }
    @Test void inventedCompositionBrandAndWeightAreRejectedEvenInsideOtherwiseValidJson()throws Exception{
        for(String invalid:List.of(VALID.replace("Подробные характеристики товара уточняются.","Шоколад изготовлен из какао, сахара и молока."),VALID.replace("\"name\":\"Шоколад\"","\"name\":\"Шоколад 100 г\""),VALID.replace("\"name\":\"Шоколад\"","\"name\":\"Шоколад Milka\""),VALID.replace("Какой состав указан на упаковке?","Подтвердите, что в составе молоко."))){
            var client=response(invalid,200);assertThatThrownBy(()->new ProductDraftModelService("test-key","openrouter/free",client).generate("Шоколад")).isInstanceOf(ProductDraftModelService.Unavailable.class).hasMessageContaining("не прошёл проверку");
        }
    }
    @Test void titleMayOnlyNormalizeOwnerWordsAndCannotIntroduceFacts()throws Exception{
        var client=response(VALID,200);assertThat(new ProductDraftModelService("test-key","openrouter/free",client).generate("  шоколад  ").name()).isEqualTo("Шоколад");
        assertThat(ProductDraftModelService.allowedNames("  MILKA   100 г ")).containsExactly("MILKA 100 г");
    }
    @Test void extraFactsAndInvalidValuesAreRejectedRatherThanReturnedAsGeneratedText()throws Exception{
        for(String invalid:List.of("not json",VALID.replace("\"name\":\"Шоколад\"","\"name\":\"Шоколад\",\"price\":100"),VALID.replace("Продукты","Несуществующая категория"),VALID.replace("Шоколад","x".repeat(301)),VALID.replace("[\"Какой состав указан на упаковке?\"]","[1]"))){
            var client=response(invalid,200);assertThatThrownBy(()->new ProductDraftModelService("test-key","openrouter/free",client).generate("Шоколад")).isInstanceOf(ProductDraftModelService.Unavailable.class).hasMessageContaining("не прошёл проверку");
        }
    }
    @Test void rateLimitAndTimeoutAreHonestFailuresWithoutFallbackText()throws Exception{
        var client=response("",429);assertThatThrownBy(()->new ProductDraftModelService("test-key","openrouter/free",client).generate("Товар")).hasMessageContaining("Лимит");verify(client,times(1)).send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
        var timeout=mock(HttpClient.class);when(timeout.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenThrow(new HttpTimeoutException("test"));assertThatThrownBy(()->new ProductDraftModelService("test-key","openrouter/free",timeout).generate("Товар")).hasMessageContaining("25 секунд");
    }
    @Test void diagnosticsIdentifySafeValidationFailureWithoutRetainingInputOrRawOutput()throws Exception{
        String raw=VALID.replace("Подробные характеристики товара уточняются.","UNVERIFIED-COMPOSITION-DO-NOT-STORE");var client=response(raw,200);var ai=new ProductDraftModelService("test-key","openrouter/free",client);
        assertThatThrownBy(()->ai.generate("Шоколад")).isInstanceOf(ProductDraftModelService.Unavailable.class);
        var status=ai.status();assertThat(status).containsEntry("lastState","FAILED").containsEntry("lastStage","SAFE_OPTIONS").containsEntry("lastFailureCode","DESCRIPTION_ENUM").containsEntry("lastHttpStatus",200).containsEntry("lastActualModel","test/model:free").containsEntry("lastFinishReason","stop");assertThat(status.get("lastAttempt")).isNotNull();
        assertThat(json.writeValueAsString(status)).doesNotContain("UNVERIFIED-COMPOSITION-DO-NOT-STORE","Шоколад","test-key","Bearer","choices","content");
    }
    @Test void http200ProviderErrorIsClassifiedBeforeModelWithoutExposingTheErrorBody()throws Exception{
        var client=mock(HttpClient.class);HttpResponse<byte[]> response=mock(HttpResponse.class);when(response.statusCode()).thenReturn(200);when(response.body()).thenReturn(json.writeValueAsBytes(Map.of("error",Map.of("code",400,"message","UNTRUSTED-PROVIDER-ERROR-BODY","metadata",Map.of("raw","PRIVATE-RAW-TEXT")))));when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(response);
        var ai=new ProductDraftModelService("test-key","openrouter/free",client);assertThatThrownBy(()->ai.generate("Шоколад")).isInstanceOf(ProductDraftModelService.Unavailable.class).hasMessageNotContaining("UNTRUSTED");
        assertThat(ai.status()).containsEntry("lastHttpStatus",200).containsEntry("lastProviderErrorCode",400).containsEntry("lastStage","PROVIDER_ERROR").containsEntry("lastFailureCode","PROVIDER_ERROR_400").containsEntry("lastActualModel","");assertThat(json.writeValueAsString(ai.status())).doesNotContain("UNTRUSTED-PROVIDER-ERROR-BODY","PRIVATE-RAW-TEXT","Шоколад","test-key");
    }
    @Test void javaRejectsDuplicateQuestionsAlthoughRequestAvoidsUnsupportedUniqueItemsKeyword()throws Exception{
        String duplicates=VALID.replace("[\"Какой состав указан на упаковке?\"]","[\"Какой состав указан на упаковке?\",\"Какой состав указан на упаковке?\"]");var ai=new ProductDraftModelService("test-key","openrouter/free",response(duplicates,200));
        assertThatThrownBy(()->ai.generate("Шоколад")).isInstanceOf(ProductDraftModelService.Unavailable.class);assertThat(ai.status()).containsEntry("lastFailureCode","QUESTIONS_ENUM");
    }
    @Test void oversizedResponseCancelsTheBodySubscription()throws Exception{
        var client=response(VALID,200);new ProductDraftModelService("test-key","openrouter/free",client).generate("Шоколад");
        var handler=org.mockito.ArgumentCaptor.forClass(HttpResponse.BodyHandler.class);verify(client).send(any(HttpRequest.class),handler.capture());
        HttpResponse.BodySubscriber<byte[]> subscriber=(HttpResponse.BodySubscriber<byte[]>)handler.getValue().apply(mock(HttpResponse.ResponseInfo.class));
        var subscription=mock(java.util.concurrent.Flow.Subscription.class);subscriber.onSubscribe(subscription);subscriber.onNext(List.of(ByteBuffer.wrap(new byte[64_001])));
        verify(subscription).cancel();assertThatThrownBy(()->subscriber.getBody().toCompletableFuture().join()).hasCauseInstanceOf(java.io.IOException.class);
    }
    private String body(HttpRequest request)throws Exception{
        var complete=new CompletableFuture<String>();var bytes=new java.io.ByteArrayOutputStream();request.bodyPublisher().orElseThrow().subscribe(new java.util.concurrent.Flow.Subscriber<ByteBuffer>(){
            public void onSubscribe(java.util.concurrent.Flow.Subscription subscription){subscription.request(Long.MAX_VALUE);}public void onNext(ByteBuffer buffer){byte[] copy=new byte[buffer.remaining()];buffer.get(copy);bytes.writeBytes(copy);}public void onError(Throwable error){complete.completeExceptionally(error);}public void onComplete(){complete.complete(bytes.toString(StandardCharsets.UTF_8));}
        });return complete.get(2,TimeUnit.SECONDS);
    }
}
