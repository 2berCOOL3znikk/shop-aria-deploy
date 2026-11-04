package com.aria;

import com.aria.entity.*;
import com.aria.repository.*;
import com.aria.service.*;
import com.aria.enums.Role;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"aria.smart-pricing.schedule-enabled=false","aria.intelligence.external-enabled=false","aria.seed-demo-catalog=false","aria.seed-demo-users=false","aria.manager.enabled=false","aria.manager.schedule-enabled=false","aria.store-sync.worker-enabled=false","spring.datasource.url=${ARIA_TEST_DATABASE_URL:jdbc:h2:mem:productcreationtest;DB_CLOSE_DELAY=-1}","spring.datasource.username=${ARIA_TEST_DATABASE_USER:sa}","spring.datasource.password=${ARIA_TEST_DATABASE_PASSWORD:}","spring.datasource.driver-class-name=${ARIA_TEST_DATABASE_DRIVER:org.h2.Driver}","spring.flyway.enabled=${ARIA_TEST_FLYWAY:false}","spring.flyway.locations=classpath:db/migration/postgresql","spring.flyway.baseline-on-migrate=true","spring.flyway.baseline-version=0","spring.jpa.hibernate.ddl-auto=${ARIA_TEST_SCHEMA_MODE:update}","logging.level.root=ERROR"})
class ProductCreationIntegrationTest {
    @Autowired Environment env;@Autowired AppUserRepository users;@Autowired PasswordEncoder passwords;
    @Autowired ProductCreationSettingsRepository settings;@Autowired ProductAiDraftRepository drafts;@Autowired ProductRepository products;@Autowired AuditEventRepository audits;
    @MockitoBean ProductDraftModelService ai;
    private String admin,user;private static final String BASE="/api/admin/control/products";
    private final tools.jackson.databind.ObjectMapper json=tools.jackson.databind.json.JsonMapper.builder().build();private final HttpClient http=HttpClient.newHttpClient();
    private static final ProductDraftModelService.Output OUTPUT=new ProductDraftModelService.Output("Шоколад","Продукты","Подробные характеристики товара уточняются.",List.of("Какой состав указан на упаковке?"),"test/model:free");
    record Reply(int status,Map<String,Object> body,String raw){long id(){return ((Number)body.get("id")).longValue();}}
    @BeforeEach void setup()throws Exception{
        drafts.deleteAll();settings.deleteAll();settings.saveAndFlush(new ProductCreationSettings());reset(ai);when(ai.status()).thenReturn(Map.of("configured",true,"model","test/model:free","textOnly",true));when(ai.generate(anyString())).thenReturn(OUTPUT);
        admin=account(Role.ADMIN);user=account(Role.USER);
    }
    private String account(Role role)throws Exception{String email=UUID.randomUUID()+"@draft.test";users.saveAndFlush(new AppUser(email,passwords.encode("testPassword123"),"Product draft test",role));return (String)call("POST","/api/auth/login",null,Map.of("email",email,"password","testPassword123")).body.get("token");}
    private Reply call(String method,String path,String token,Object data)throws Exception{
        var request=HttpRequest.newBuilder(URI.create("http://localhost:"+env.getProperty("local.server.port")+path)).timeout(java.time.Duration.ofSeconds(35)).header("Content-Type","application/json");if(token!=null)request.header("Authorization","Bearer "+token);
        request.method(method,data==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(data)));var response=http.send(request.build(),HttpResponse.BodyHandlers.ofString());Map<String,Object> body=response.body().startsWith("{")?json.readValue(response.body(),Map.class):Map.of();return new Reply(response.statusCode(),body,response.body());
    }
    private Reply consent(boolean enabled,int budget)throws Exception{var current=call("GET",BASE+"/ai-settings",admin,null);return call("PUT",BASE+"/ai-settings",admin,Map.of("revision",current.body.get("revision"),"textConsentEnabled",enabled,"dailyBudget",budget));}
    private Map<String,Object> product(){return Map.of("name","Шоколад","category","Продукты","description","Проверенный владельцем текст.","unitPrice",1000,"unit","шт","lowStockThreshold",5);}
    @Test void defaultsAreOffAndSettingsAndDraftsRequireAdmin()throws Exception{
        var config=call("GET",BASE+"/ai-settings",admin,null);assertThat(config.body).containsEntry("textConsentEnabled",false).containsEntry("dailyBudget",5).containsEntry("textOnly",true);assertThat(config.raw).doesNotContain("test-key","Authorization","encrypted");
        assertThat(call("GET",BASE+"/ai-settings",null,null).status).isEqualTo(401);assertThat(call("GET",BASE+"/ai-settings",user,null).status).isEqualTo(403);assertThat(call("POST",BASE+"/ai-drafts",user,Map.of("name","Товар")).status).isEqualTo(403);
        assertThat(call("POST",BASE+"/ai-drafts",admin,Map.of("name","Товар")).status).isEqualTo(409);verify(ai,never()).generate(anyString());
    }
    @Test void draftRequiresConfirmationAndCreatesOneProductWithAutomaticSkuAndProvenance()throws Exception{
        assertThat(consent(true,5).status).isEqualTo(200);long before=products.count();var draft=call("POST",BASE+"/ai-drafts",admin,Map.of("name","Шоколад"));assertThat(draft.status).as(draft.raw).isEqualTo(200);assertThat(draft.body).containsEntry("source","AI").containsEntry("verificationStatus","UNCONFIRMED_TEXT");assertThat(products.count()).isEqualTo(before);
        String confirm=BASE+"/ai-drafts/"+draft.id()+"/confirm";assertThat(call("POST",confirm,admin,Map.of("confirmed",false,"product",product())).status).isEqualTo(400);
        var created=call("POST",confirm,admin,Map.of("confirmed",true,"product",product()));assertThat(created.status).as(created.raw).isEqualTo(200);assertThat((String)created.body.get("sku")).startsWith("ARIA-AUTO-");assertThat(created.body).containsEntry("quantityOnHand",0);assertThat(products.count()).isEqualTo(before+1);
        var provenance=call("GET",BASE+"/"+created.id()+"/ai-provenance",admin,null);assertThat(provenance.body).containsEntry("verificationStatus","OWNER_REVIEWED_TEXT").containsEntry("description",OUTPUT.description()).containsEntry("productId",created.body.get("id"));assertThat(provenance.body.get("confirmedAt")).isNotNull();
        assertThat(call("POST",confirm,admin,Map.of("confirmed",true,"product",product())).status).isEqualTo(409);assertThat(products.count()).isEqualTo(before+1);assertThat(audits.findAll()).anyMatch(a->a.getAction().equals("PRODUCT_AI_TEXT_CONFIRMED")&&a.getResource().equals("product:"+created.id()));
        var edit=call("PUT",BASE+"/"+created.id(),admin,product());assertThat(edit.status).as(edit.raw).isEqualTo(200);assertThat(edit.body.get("sku")).isEqualTo(created.body.get("sku"));
    }
    @Test void quotaAndCooldownPersistAcrossServiceCallsAndFailedAttemptsAreCounted()throws Exception{
        consent(true,1);when(ai.generate(anyString())).thenThrow(new ProductDraftModelService.Unavailable("ИИ временно недоступен."));assertThat(call("POST",BASE+"/ai-drafts",admin,Map.of("name","Товар")).status).isEqualTo(503);
        var saved=drafts.findAll().getFirst();assertThat(saved.status).isEqualTo("FAILED");assertThat(saved.name).isNull();assertThat(saved.model).isNull();assertThat(call("GET",BASE+"/ai-settings",admin,null).body).containsEntry("budgetUsed",1).containsEntry("remaining",0);
        assertThat(call("POST",BASE+"/ai-drafts",admin,Map.of("name","Товар")).status).isEqualTo(429);var config=settings.findById(1L).orElseThrow();config.lastRequestAt=config.lastRequestAt.minusMinutes(2);settings.saveAndFlush(config);
        assertThat(call("POST",BASE+"/ai-drafts",admin,Map.of("name","Товар")).status).isEqualTo(429);verify(ai,times(1)).generate(anyString());
    }
    @Test void unsafeAdapterTextIsRejectedAndLegacyDraftCannotCreateProductButRemainsForAudit()throws Exception{
        consent(true,5);long before=products.count();var unsafe=new ProductDraftModelService.Output("Шоколад","Продукты","Изготовлен из какао, сахара и молока.",List.of("Какой состав указан на упаковке?"),"test/model:free");when(ai.generate(anyString())).thenReturn(unsafe);
        assertThat(call("POST",BASE+"/ai-drafts",admin,Map.of("name","Шоколад")).status).isEqualTo(503);assertThat(drafts.findAll().getFirst().status).isEqualTo("FAILED");assertThat(products.count()).isEqualTo(before);
        var legacy=new ProductAiDraft();legacy.inputName="Шоколад";legacy.status="DRAFT";legacy.consentRevision=settings.findById(1L).orElseThrow().consentRevision;legacy.name=unsafe.name();legacy.category=unsafe.category();legacy.description=unsafe.description();legacy.questionsJson=json.writeValueAsString(unsafe.questions());legacy.model=unsafe.model();drafts.saveAndFlush(legacy);
        assertThat(call("POST",BASE+"/ai-drafts/"+legacy.id+"/confirm",admin,Map.of("confirmed",true,"product",product())).status).isEqualTo(409);assertThat(products.count()).isEqualTo(before);assertThat(drafts.findById(legacy.id).orElseThrow().description).isEqualTo(unsafe.description());
    }
    @Test void consentRevokedDuringInferencePreventsSavingAnswerWithoutHoldingDatabaseLocks()throws Exception{
        consent(true,5);var started=new CountDownLatch(1);var finish=new CountDownLatch(1);when(ai.generate(anyString())).thenAnswer(invocation->{started.countDown();if(!finish.await(10,TimeUnit.SECONDS))throw new AssertionError("Test inference blocked");return OUTPUT;});
        var executor=Executors.newSingleThreadExecutor();try{
            var response=executor.submit(()->call("POST",BASE+"/ai-drafts",admin,Map.of("name","Шоколад")));assertThat(started.await(5,TimeUnit.SECONDS)).isTrue();assertThat(consent(false,5).status).isEqualTo(200);finish.countDown();assertThat(response.get(10,TimeUnit.SECONDS).status).isEqualTo(409);
            var draft=drafts.findAll().getFirst();assertThat(draft.status).isEqualTo("CANCELLED");assertThat(draft.description).isNull();assertThat(draft.model).isNull();assertThat(audits.findAll()).anyMatch(a->a.getAction().equals("PRODUCT_AI_TEXT_CONSENT_REVOKED"));
        }finally{finish.countDown();executor.shutdownNow();}
    }
    @Test void manualCodesRemainSupportedAndAutomaticCodesAreUniqueUnderConcurrentCreation()throws Exception{
        var manual=new HashMap<>(product());manual.put("sku","MY-"+UUID.randomUUID());assertThat(call("POST",BASE,admin,manual).status).isEqualTo(200);manual.put("sku","ARIA-AUTO-9999999999999");assertThat(call("POST",BASE,admin,manual).status).isEqualTo(400);
        var executor=Executors.newFixedThreadPool(4);try{var futures=new ArrayList<Future<Reply>>();for(int i=0;i<8;i++)futures.add(executor.submit(()->call("POST",BASE,admin,product())));var codes=new HashSet<>();for(var f:futures){var response=f.get(20,TimeUnit.SECONDS);assertThat(response.status).as(response.raw).isEqualTo(200);assertThat(codes.add(response.body.get("sku"))).isTrue();}assertThat(codes).hasSize(8);}finally{executor.shutdownNow();}
    }
}
