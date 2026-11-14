package com.aria;

import com.aria.service.*;
import com.aria.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"aria.company-signup.enabled=false","aria.company-signup.intake-only=false","aria.smart-pricing.schedule-enabled=false","aria.intelligence.external-enabled=false","aria.seed-demo-catalog=false","aria.seed-demo-users=false","aria.manager.enabled=false","aria.manager.schedule-enabled=false","aria.store-sync.worker-enabled=false","spring.datasource.url=${ARIA_TEST_DATABASE_URL:jdbc:h2:mem:companyIntake;DB_CLOSE_DELAY=-1}","spring.datasource.username=${ARIA_TEST_DATABASE_USER:sa}","spring.datasource.password=${ARIA_TEST_DATABASE_PASSWORD:}","spring.datasource.driver-class-name=${ARIA_TEST_DATABASE_DRIVER:org.h2.Driver}","spring.flyway.enabled=${ARIA_TEST_FLYWAY:false}","spring.flyway.locations=classpath:db/migration/postgresql","spring.flyway.baseline-on-migrate=true","spring.flyway.baseline-version=0","spring.jpa.hibernate.ddl-auto=${ARIA_TEST_SCHEMA_MODE:update}","logging.level.root=ERROR"})
class CompanyIntakeIntegrationTest {
 // Build the intake service over the temporary database without relabelling its demo binding.
 // The real guarded staging configuration and HTTP path are exercised separately in the browser.
 private CompanySignupService signup;
 private CompanyProvisioningService provisioning;
 @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
 @Autowired com.aria.integration.IntegrationVault vault;
 @Autowired LocalWorkspaceProbe probe;
 private <T> T tx(java.util.function.Supplier<T> task){return new org.springframework.transaction.support.TransactionTemplate(transactions).execute(status->task.get());}
 @BeforeEach void intake(){signup=new CompanySignupService(false,true,"aria-v4-staging",rows,captcha,audit);org.springframework.test.util.ReflectionTestUtils.setField(signup,"databaseDriver",env.getProperty("spring.datasource.driver-class-name"));provisioning=new CompanyProvisioningService(signup,rows,vault,probe,audit,transactions);}
 @Autowired CompanyRegistrationRequestRepository rows;
 @Autowired AppUserRepository users;
 @Autowired AuditService audit;
 @Autowired CaptchaService captcha;
 @Autowired Environment env;
 private final tools.jackson.databind.ObjectMapper json=tools.jackson.databind.json.JsonMapper.builder().build();
 private final HttpClient client=HttpClient.newHttpClient();
 private Map<String,String> challenge(){return signup.challenge(UUID.randomUUID().toString());}
 private String answer(Map<String,String> c){return Integer.toString(java.util.regex.Pattern.compile("\\d+").matcher(c.get("question")).results().mapToInt(m->Integer.parseInt(m.group())).sum());}
 private String token(){return UUID.randomUUID().toString().replace("-","")+UUID.randomUUID().toString().replace("-","");}
 private Map<String,Object> create(String id,String code){var c=challenge();return tx(()->signup.submit(id,code,"Учебная онлайн-заявка","Учебный заявитель","intake-review@example.test",true,true,"",c.get("id"),answer(c),UUID.randomUUID().toString()));}
 private HttpResponse<String> get(String path)throws Exception{return client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+env.getProperty("local.server.port")+path)).GET().build(),HttpResponse.BodyHandlers.ofString());}
 @Test void onlineIntakeDoesNotOpenBusinessOrOperatorApis()throws Exception{
  var response=(org.springframework.http.ResponseEntity<?>)new com.aria.controller.CompanySignupController(signup,provisioning).capabilities();assertThat(response.getHeaders().getCacheControl()).contains("no-store");
  var capabilities=(Map<?,?>)response.getBody();assertThat(capabilities.get("enabled")).isEqualTo(true);assertThat(capabilities.get("intakeOnly")).isEqualTo(true);assertThat(capabilities.get("canProvision")).isEqualTo(false);
  assertThat(get("/api/products").statusCode()).isEqualTo(401);assertThat(get("/api/admin/company-signup/requests").statusCode()).isEqualTo(401);assertThat(get("/api/admin/control/stores").statusCode()).isEqualTo(401);
 }
 @Test void applicationAndLostResponseRecoveryNeverCreateLoginOrWorkspace(){
  long before=users.count();String id=UUID.randomUUID().toString(),code=token();var created=create(id,code);long count=rows.count();
  var retry=tx(()->signup.submit(id,code,"Учебная онлайн-заявка","Учебный заявитель","INTAKE-REVIEW@EXAMPLE.TEST",true,true,"","expired","bad","retry-client"));
  assertThat(retry).containsEntry("id",id).containsEntry("status","SUBMITTED").containsEntry("workspaceReady",false).containsEntry("accountCreated",false).containsEntry("emailAutomaticallyVerified",false);
  assertThat(retry).doesNotContainKeys("token","accessHash","ownerEmail","workspaceUrl");assertThat(rows.count()).isEqualTo(count);assertThat(users.count()).isEqualTo(before);assertThat(rows.findById(id).orElseThrow().accessHash).isEqualTo(CompanySignupService.hash(code));assertThat(signup.status(id,code,"owner")).containsEntry("id",created.get("id")).containsEntry("status","SUBMITTED");
 }
 @Test void applicationCodesAndConsentRemainSeparate(){
  String first=UUID.randomUUID().toString(),second=UUID.randomUUID().toString(),code=token();create(first,code);create(second,token());
  assertThatThrownBy(()->signup.status(second,code,"foreign")).hasMessageContaining("404");assertThatThrownBy(()->signup.status(first,null,"missing")).hasMessageContaining("404");
  assertThatThrownBy(()->signup.submit(UUID.randomUUID().toString(),token(),"Company","Owner","owner@example.test",true,false,"","none","0","consent")).isInstanceOf(IllegalArgumentException.class);
  assertThatThrownBy(()->create(UUID.randomUUID().toString(),code)).hasMessageContaining("409");
 }
 @Test void verifiedApplicationStillCannotProvisionOrActivateAnExistingCompany(){
  String id=UUID.randomUUID().toString(),code=token();create(id,code);long accounts=users.count();var checking=tx(()->signup.review(id,0,"IN_REVIEW","Synthetic review, no business environment",false));var verified=tx(()->signup.review(id,((Number)checking.get("version")).longValue(),"VERIFIED","Synthetic contact review only, no real company",true));long version=((Number)verified.get("version")).longValue();
  assertThatThrownBy(()->provisioning.prepare(id,version)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");assertThatThrownBy(()->provisioning.verify(id,version,"http://127.0.0.1:8089")).hasMessageContaining("409");assertThatThrownBy(()->provisioning.refresh(id,code,"owner")).hasMessageContaining("409");
  var r=rows.findById(id).orElseThrow();assertThat(r.setupSecretCipher).isNull();assertThat(r.workspaceBinding).isNull();assertThat(users.count()).isEqualTo(accounts);assertThat(signup.status(id,code,"owner")).containsEntry("status","VERIFIED").containsEntry("workspaceReady",false).containsEntry("accountCreated",false);
 }
 @Test void simultaneousRetriesSerializeBeforeOneTimeCaptcha()throws Exception{
  Assumptions.assumeTrue("org.postgresql.Driver".equals(env.getProperty("spring.datasource.driver-class-name")),"PostgreSQL transaction lock requires PostgreSQL");
  String id=UUID.randomUUID().toString(),code=token();var c=challenge();String answer=answer(c);long count=rows.count();var barrier=new CountDownLatch(1);
  java.util.function.Supplier<Map<String,Object>> send=()->{try{barrier.await();return tx(()->signup.submit(id,code,"Concurrent company","Owner","concurrent@example.test",true,true,"",c.get("id"),answer,UUID.randomUUID().toString()));}catch(InterruptedException e){Thread.currentThread().interrupt();throw new RuntimeException(e);}};
  var a=CompletableFuture.supplyAsync(send);var b=CompletableFuture.supplyAsync(send);barrier.countDown();assertThat(a.get(20,TimeUnit.SECONDS)).containsEntry("id",id);assertThat(b.get(20,TimeUnit.SECONDS)).containsEntry("id",id);assertThat(rows.count()).isEqualTo(count+1);
 }
 @Test void intakeModeCannotBeEnabledInAnotherCompany(){
  for(String binding:List.of("demo","woo-e2e","aria-registration","business-other"))assertThatThrownBy(()->new CompanySignupService(false,true,binding,rows,captcha,audit)).isInstanceOf(IllegalStateException.class);
  assertThatThrownBy(()->new CompanySignupService(true,true,"aria-v4-staging",rows,captcha,audit)).isInstanceOf(IllegalStateException.class);
 }
}
