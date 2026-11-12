package com.aria;

import com.aria.entity.AppUser;
import com.aria.enums.Role;
import com.aria.repository.AppUserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.net.*;
import java.net.http.*;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"aria.manager.enabled=false","aria.manager.schedule-enabled=false","aria.manager.activity-enabled=false","aria.store-sync.worker-enabled=false","aria.smart-pricing.schedule-enabled=false","aria.connected-pricing.schedule-enabled=false","aria.intelligence.external-enabled=false","aria.seed-demo-catalog=false","aria.seed-demo-users=false","spring.datasource.url=${ARIA_TEST_DATABASE_URL:jdbc:h2:mem:managerSetup;DB_CLOSE_DELAY=-1}","spring.datasource.username=${ARIA_TEST_DATABASE_USER:sa}","spring.datasource.password=${ARIA_TEST_DATABASE_PASSWORD:}","spring.datasource.driver-class-name=${ARIA_TEST_DATABASE_DRIVER:org.h2.Driver}","spring.flyway.enabled=${ARIA_TEST_FLYWAY:false}","spring.flyway.locations=classpath:db/migration/postgresql","spring.flyway.baseline-on-migrate=true","spring.flyway.baseline-version=0","spring.jpa.hibernate.ddl-auto=${ARIA_TEST_SCHEMA_MODE:update}","logging.level.root=ERROR"})
class ManagerSetupIntegrationTest {
 @Autowired Environment env;@Autowired AppUserRepository users;@Autowired PasswordEncoder passwords;
 private final tools.jackson.databind.ObjectMapper json=tools.jackson.databind.json.JsonMapper.builder().build();private final HttpClient http=HttpClient.newHttpClient();private String admin;
 private static final String ROOT="/api/admin/control";
 @BeforeEach void reset()throws Exception{admin=account(Role.ADMIN);assertThat(call("POST",ROOT+"/manager/stop",admin,null).statusCode()).isEqualTo(200);}
 @Test void setupIsAdminOnlyAndRejectsInvalidNestedLimits()throws Exception{
  var b=body("ADVISOR",false,false,false);assertThat(call("POST",ROOT+"/manager/setup",null,b).statusCode()).isEqualTo(401);assertThat(call("POST",ROOT+"/manager/setup",account(Role.USER),b).statusCode()).isEqualTo(403);
  map(b,"pricing").put("maximumDiscount",91);assertThat(call("POST",ROOT+"/manager/setup",admin,b).statusCode()).isEqualTo(400);
  assertThat(call("POST",ROOT+"/manager/setup",admin,Map.of()).statusCode()).isEqualTo(400);
 }
 @Test void dataAndAutomaticRequestsNeedSeparateExplicitConsent()throws Exception{
  var before=pricing();var b=body("ADVISOR",true,true,false);
  assertThat(call("POST",ROOT+"/manager/setup",admin,b).statusCode()).isEqualTo(400);
  b.put("dataConsentConfirmed",true);assertThat(call("POST",ROOT+"/manager/setup",admin,b).statusCode()).isEqualTo(400);
  assertThat(pricing()).containsEntry("version",before.get("version")).containsEntry("externalAiEnabled",false);assertThat(manager()).containsEntry("mode","OFF");
 }
 @Test void laterManagerRejectionRollsBackPricingConsentAndAuditInSameTransaction()throws Exception{
  var before=pricing();var state=manager();var b=body("AUTOPILOT",true,true,false);b.put("dataConsentConfirmed",true);b.put("automationConsentConfirmed",true);
  var response=call("POST",ROOT+"/manager/setup",admin,b);assertThat(response.statusCode()).as(response.body()).isEqualTo(409);
  assertThat(pricing()).containsEntry("version",before.get("version")).containsEntry("externalAiEnabled",false).containsEntry("mode","RECOMMEND");assertThat(manager()).containsEntry("revision",state.get("revision")).containsEntry("mode","OFF");
 }
 @Test void reviewedSetupUsesExistingLimitsAndStaleSubmissionCannotEnableAgain()throws Exception{
  var b=body("AUTOPILOT",true,true,true);b.put("dataConsentConfirmed",true);b.put("automationConsentConfirmed",true);
  var saved=call("POST",ROOT+"/manager/setup",admin,b);assertThat(saved.statusCode()).as(saved.body()).isEqualTo(200);assertThat(read(saved)).containsEntry("queued",false);
  assertThat(pricing()).containsEntry("mode","AUTOPILOT").containsEntry("externalAiEnabled",true).containsEntry("allowLowConfidence",false).containsEntry("scheduledAiEnabled",false).containsEntry("maximumDiscount",15);
  assertThat(manager()).containsEntry("mode","AUTOPILOT").containsEntry("automaticAiConsent",true).containsEntry("socialDrafts",false).containsEntry("purchaseDrafts",false);
  var version=pricing().get("version");assertThat(call("POST",ROOT+"/manager/setup",admin,b).statusCode()).isEqualTo(409);assertThat(pricing()).containsEntry("version",version);
  assertThat(call("POST",ROOT+"/manager/stop",admin,null).statusCode()).isEqualTo(200);assertThat(pricing()).containsEntry("externalAiEnabled",false).containsEntry("mode","RECOMMEND");assertThat(manager()).containsEntry("mode","OFF").containsEntry("automaticAiConsent",false);
 }
 private Map<String,Object> body(String mode,boolean external,boolean automatic,boolean confirmed)throws Exception{
  var p=pricing();var m=manager();var pricing=new LinkedHashMap<String,Object>();pricing.put("version",p.get("version"));pricing.put("mode","RECOMMEND");pricing.put("maximumDiscount",15);pricing.put("cooldownHours",24);pricing.put("allowLowConfidence",false);pricing.put("allowRestore",false);pricing.put("allowedCategories",List.of("test"));pricing.put("externalAiEnabled",external);pricing.put("scheduledAiEnabled",automatic);pricing.put("aiDailyBudget",3);
  var manager=new LinkedHashMap<String,Object>();manager.put("revision",m.get("revision"));manager.put("mode",mode);manager.put("automaticAiConsent",automatic);manager.put("socialDrafts",false);manager.put("purchaseDrafts",false);manager.put("intervalMinutes",30);manager.put("dailyDraftBudget",0);manager.put("pricingVersion",p.get("version"));manager.put("policyConfirmed",confirmed);
  var result=new LinkedHashMap<String,Object>();result.put("pricing",pricing);result.put("manager",manager);result.put("dataConsentConfirmed",false);result.put("automationConsentConfirmed",false);return result;
 }
 private Map<String,Object> pricing()throws Exception{return read(call("GET",ROOT+"/smart-pricing/settings",admin,null));}
 private Map<String,Object> manager()throws Exception{return map(read(call("GET",ROOT+"/manager",admin,null)),"settings");}
 private Map<String,Object> read(HttpResponse<String> response){return json.readValue(response.body(),Map.class);}
 private static Map<String,Object> map(Map<String,Object> value,String key){return (Map<String,Object>)value.get(key);}
 private String account(Role role)throws Exception{var email="setup-"+UUID.randomUUID()+"@test.local";users.save(new AppUser(email,passwords.encode("TeachingSetup123"),"Setup test",role));return (String)read(call("POST","/api/auth/login",null,Map.of("email",email,"password","TeachingSetup123"))).get("token");}
 private HttpResponse<String> call(String method,String path,String token,Object body)throws Exception{var request=HttpRequest.newBuilder(URI.create("http://localhost:"+env.getProperty("local.server.port")+path));if(token!=null)request.header("Authorization","Bearer "+token);if(body!=null)request.header("Content-Type","application/json");request.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));return http.send(request.build(),HttpResponse.BodyHandlers.ofString());}
}
