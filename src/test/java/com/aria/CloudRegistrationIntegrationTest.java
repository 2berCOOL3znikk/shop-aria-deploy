package com.aria;
import com.aria.entity.*;
import com.aria.repository.*;
import com.aria.service.*;
import com.aria.integration.IntegrationVault;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.junit.jupiter.api.*;
import java.util.*;
import java.time.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real isolated PostgreSQL transactions; the remote interface is explicitly mocked, not external E2E. */
@SpringBootTest(properties={"aria.seed-demo-catalog=false","aria.seed-demo-users=false","aria.manager.enabled=false","aria.manager.schedule-enabled=false","aria.intelligence.external-enabled=false","aria.store-sync.worker-enabled=false","aria.smart-pricing.schedule-enabled=false","spring.datasource.url=${ARIA_TEST_DATABASE_URL:jdbc:h2:mem:cloudRegistration;DB_CLOSE_DELAY=-1}","spring.datasource.username=${ARIA_TEST_DATABASE_USER:sa}","spring.datasource.password=${ARIA_TEST_DATABASE_PASSWORD:}","spring.datasource.driver-class-name=${ARIA_TEST_DATABASE_DRIVER:org.h2.Driver}","spring.flyway.enabled=${ARIA_TEST_FLYWAY:false}","spring.flyway.locations=classpath:db/migration/postgresql","spring.flyway.baseline-on-migrate=true","spring.flyway.baseline-version=0","spring.jpa.hibernate.ddl-auto=${ARIA_TEST_SCHEMA_MODE:update}","logging.level.root=ERROR"})
class CloudRegistrationIntegrationTest {
 @Autowired CloudRegistrationRepository rows;
 @Autowired WorkspacePoolSlotRepository slots;
 @Autowired CompanyRegistrationRequestRepository locks;
 @Autowired CaptchaService captcha;
 @Autowired AuditService audit;
 @Autowired PlatformTransactionManager manager;
 @Autowired Environment environment;
 @Autowired AppUserRepository users;
 private CloudRegistrationService service;
 private CloudWorkspaceTransport remote;
 private IntegrationVault vault;
 private final String password="Owner-password-12345";
 private final Map<String,Map<String,Object>> installed=new ConcurrentHashMap<>();
 @BeforeEach void setup(){
  vault=new IntegrationVault(Base64.getEncoder().encodeToString(new byte[32]));remote=mock(CloudWorkspaceTransport.class);
  when(remote.exchange(anyString(),anyString(),anyMap())).thenAnswer(call->{Map<String,Object> r=call.getArgument(2);String binding=r.get("binding").toString(),action=r.get("action").toString();if(action.equals("INSTALL")){installed.compute(binding,(key,old)->{if(old!=null&&!old.get("registrationId").equals(r.get("registrationId")))throw new IllegalStateException("Already owned");return Map.of("binding",binding,"registrationId",r.get("registrationId"),"emailHash",CompanySignupService.hash(r.get("email").toString()),"companyHash",CompanySignupService.hash(r.get("companyName").toString()),"ready",true,"empty",false);});}return installed.getOrDefault(binding,Map.of("binding",binding,"empty",true,"ready",false));});
  var env=new MockEnvironment().withProperty("aria.saas.gateway-enabled","true").withProperty("aria.saas.auto-prepare","false").withProperty("aria.organization.key","aria-v4-staging").withProperty("aria.integrations.encryption-key",Base64.getEncoder().encodeToString(new byte[32])).withProperty("spring.datasource.driver-class-name",environment.getProperty("spring.datasource.driver-class-name"));
  var intake=new CompanySignupService(false,true,"aria-v4-staging",locks,captcha,audit);service=new CloudRegistrationService(env,rows,slots,locks,vault,remote,captcha,intake,new BCryptPasswordEncoder(),audit,manager);
  org.springframework.test.util.ReflectionTestUtils.setField(service,"nativeUsers",users);
  rows.deleteAll();slots.deleteAll();
 }
 @AfterEach void close(){service.destroy();}
 private String code(){return CloudWorkspaceProof.random();}
 private Map<?,?> slot(String admission){String id=UUID.randomUUID().toString(),binding="business-"+id.replace("-","");return (Map<?,?>)service.addSlot(id,binding,"https://aria-company-a-2ber-stg.onrender.com",code(),admission);}
 private Map<String,String> challenge(){return (Map<String,String>)service.challenge(UUID.randomUUID().toString());}
 private String answer(Map<String,String> c){return Integer.toString(java.util.regex.Pattern.compile("\\d+").matcher(c.get("question")).results().mapToInt(x->Integer.parseInt(x.group())).sum());}
 private CloudRegistrationService.Signup signup(String id,String access,String admission,String email){var c=challenge();return new CloudRegistrationService.Signup(id,access,admission,email,password,"New isolated company",true,true,c.get("id"),answer(c));}
 @Test void activeMeansIndependentReadbackAndNeverCreatesAnOwnerInTheCoordinator(){
  String admission=code(),id=UUID.randomUUID().toString(),access=code();slot(admission);long accounts=users.count();var initial=(Map<?,?>)service.register(signup(id,access,admission,"owner@example.test"),"client");assertThat(initial.get("workspaceReady")).isEqualTo(false);
  service.prepare(id);var actual=(Map<?,?>)service.status(id,access,"owner");assertThat(actual.get("status")).isEqualTo("ACTIVE");assertThat(actual.get("workspaceReady")).isEqualTo(true);assertThat(users.count()).isEqualTo(accounts);assertThat(rows.findById(id).orElseThrow().credentialCipher).isNull();assertThat(slots.findAll().getFirst().state).isEqualTo("CLAIMED");assertThat(service.loginSlot("OWNER@EXAMPLE.TEST")).isPresent();verify(remote,times(1)).exchange(anyString(),anyString(),argThat(m->m.get("action").equals("ATTEST")));
 }
 @Test void missingInvitationOrConsentCannotReserveCapacity(){
  String admission=code();slot(admission);String id=UUID.randomUUID().toString(),access=code();var bad=signup(id,access,code(),"wrong@example.test");assertThatThrownBy(()->service.register(bad,"bad")).hasMessageContaining("403");assertThat(rows.existsById(id)).isFalse();assertThat(slots.countByState("AVAILABLE")).isEqualTo(1);
  var good=signup(id,access,admission,"owner@example.test");var noConsent=new CloudRegistrationService.Signup(good.id(),good.token(),good.admissionCode(),good.email(),good.password(),good.companyName(),false,true,good.captchaId(),good.captchaAnswer());assertThatThrownBy(()->service.register(noConsent,"bad")).isInstanceOf(IllegalArgumentException.class);assertThat(rows.existsById(id)).isFalse();
 }
 @Test void lostConfirmationKeepsTheSameAllocationAndRetryDoesNotInstallTwice(){
  String admission=code(),id=UUID.randomUUID().toString(),access=code();slot(admission);var input=signup(id,access,admission,"owner@example.test");service.register(input,"one");String slot=rows.findById(id).orElseThrow().slotId;
  doAnswer(call->{throw new IllegalStateException("Connection lost after remote commit");}).when(remote).exchange(anyString(),anyString(),argThat(m->m.get("action").equals("ATTEST")));
  service.prepare(id);assertThat(rows.findById(id).orElseThrow().status).isEqualTo("PREPARING");assertThat(installed).hasSize(1);assertThat(slots.findById(slot).orElseThrow().state).isEqualTo("RESERVED");
  doAnswer(call->{Map<String,Object> req=call.getArgument(2);return installed.get(req.get("binding"));}).when(remote).exchange(anyString(),anyString(),argThat(m->m.get("action").equals("ATTEST")));
  service.prepare(id);assertThat(rows.findById(id).orElseThrow().status).isEqualTo("ACTIVE");assertThat(rows.findById(id).orElseThrow().slotId).isEqualTo(slot);assertThat(installed).hasSize(1);
  assertThat(((Map<?,?>)service.register(input,"lost-response")).get("workspaceReady")).isEqualTo(true);assertThat(rows.count()).isEqualTo(1);
 }
 @Test void wrongReadbackNeverMarksAWorkspaceActiveOrReleasesItToSomeoneElse(){
  String admission=code(),id=UUID.randomUUID().toString(),access=code();slot(admission);service.register(signup(id,access,admission,"owner@example.test"),"one");
  doReturn(Map.of("ready",true,"binding","business-"+"f".repeat(32),"registrationId",id,"emailHash",CompanySignupService.hash("other@example.test"),"companyHash",CompanySignupService.hash("Wrong company"))).when(remote).exchange(anyString(),anyString(),argThat(m->m.get("action").equals("ATTEST")));
  for(int n=0;n<4;n++)service.prepare(id);var r=rows.findById(id).orElseThrow();assertThat(r.status).isEqualTo("RETRYABLE");assertThat(r.credentialCipher).isNotBlank();assertThat(slots.findById(r.slotId).orElseThrow().state).isEqualTo("RESERVED");assertThatThrownBy(()->service.loginSlot(r.email)).hasMessageContaining("503");
 }
 @Test void pendingRecoveryRequiresPasswordAndCaptchaThenRotatesOnlyTheStatusCode(){
  String admission=code(),id=UUID.randomUUID().toString(),access=code();slot(admission);service.register(signup(id,access,admission,"owner@example.test"),"one");String slot=rows.findById(id).orElseThrow().slotId;
  var bad=challenge();assertThatThrownBy(()->service.recover("owner@example.test","wrong",bad.get("id"),answer(bad),"recovery")).hasMessageContaining("404");var c=challenge();var recovered=(Map<?,?>)service.recover("owner@example.test",password,c.get("id"),answer(c),"recovery");String fresh=recovered.get("token").toString();assertThat(fresh).isNotEqualTo(access);assertThatThrownBy(()->service.status(id,access,"old")).hasMessageContaining("404");assertThat(((Map<?,?>)service.status(id,fresh,"new")).get("workspaceReady")).isEqualTo(false);assertThat(rows.findById(id).orElseThrow().slotId).isEqualTo(slot);
 }
 @Test void concurrentRequestsCannotSpendOneInvitationTwice()throws Exception{
  Assumptions.assumeTrue("org.postgresql.Driver".equals(environment.getProperty("spring.datasource.driver-class-name")));String admission=code();slot(admission);var first=signup(UUID.randomUUID().toString(),code(),admission,"one@example.test");var second=signup(UUID.randomUUID().toString(),code(),admission,"two@example.test");var start=new CountDownLatch(1);
  java.util.function.Function<CloudRegistrationService.Signup,Boolean> send=in->{try{start.await();service.register(in,UUID.randomUUID().toString());return true;}catch(Exception e){return false;}};var a=CompletableFuture.supplyAsync(()->send.apply(first));var b=CompletableFuture.supplyAsync(()->send.apply(second));start.countDown();assertThat(List.of(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);assertThat(rows.count()).isEqualTo(1);assertThat(slots.countByState("RESERVED")).isEqualTo(1);
 }
 @Test void anInvitationCannotClaimAnExistingPlatformEmailWithoutThatUsersSession(){
  String existingEmail=users.findAll().getFirst().getEmail(),admission=code();slot(admission);String id=UUID.randomUUID().toString(),access=code();var input=signup(id,access,admission,existingEmail);assertThatThrownBy(()->service.register(input,"external")).hasMessageContaining("409");assertThat(rows.existsById(id)).isFalse();assertThat(slots.countByState("AVAILABLE")).isEqualTo(1);
  service.register(input,"same-owner",existingEmail);assertThat(rows.findById(id).orElseThrow().status).isEqualTo("PREPARING");
 }
 @Test void changedCompanyAndPasswordCannotCollideInAnIdempotencyFingerprint(){
  String admission=code(),id=UUID.randomUUID().toString(),access=code();slot(admission);var c=challenge();var original=new CloudRegistrationService.Signup(id,access,admission,"owner@example.test",password,"A\nB",true,true,c.get("id"),answer(c));service.register(original,"first");
  var altered=new CloudRegistrationService.Signup(id,access,admission,"owner@example.test","B\n"+password,"A",true,true,"expired","0");assertThatThrownBy(()->service.register(altered,"retry")).hasMessageContaining("409");assertThat(rows.count()).isEqualTo(1);assertThat(rows.findById(id).orElseThrow().companyName).isEqualTo("A\nB");
 }
 @Test void recoveryDuringFinalConfirmationDoesNotLoseVerifiedActivation(){
  String admission=code(),id=UUID.randomUUID().toString(),access=code();slot(admission);service.register(signup(id,access,admission,"owner@example.test"),"one");
  doThrow(new IllegalStateException("Temporarily unavailable")).when(remote).exchange(anyString(),anyString(),argThat(m->m.get("action").equals("ATTEST")));for(int n=0;n<3;n++)service.prepare(id);
  var recovered=new java.util.concurrent.atomic.AtomicReference<String>();
  doAnswer(call->{var c=challenge();var result=(Map<?,?>)service.recover("owner@example.test",password,c.get("id"),answer(c),"recovery");recovered.set(result.get("token").toString());Map<String,Object> input=call.getArgument(2);return installed.get(input.get("binding"));}).when(remote).exchange(anyString(),anyString(),argThat(m->m.get("action").equals("ATTEST")));
  service.prepare(id);assertThat(rows.findById(id).orElseThrow().attempts).isEqualTo(4);assertThat(((Map<?,?>)service.status(id,recovered.get(),"new")).get("workspaceReady")).isEqualTo(true);assertThatThrownBy(()->service.status(id,access,"old")).hasMessageContaining("404");assertThat(installed).hasSize(1);
 }
 @Test void recoveryDuringFinalFailureStillAllowsPreparationToBeResumed(){
  String admission=code(),id=UUID.randomUUID().toString(),access=code();slot(admission);service.register(signup(id,access,admission,"owner@example.test"),"one");
  doThrow(new IllegalStateException("Temporarily unavailable")).when(remote).exchange(anyString(),anyString(),argThat(m->m.get("action").equals("ATTEST")));for(int n=0;n<3;n++)service.prepare(id);
  var recovered=new java.util.concurrent.atomic.AtomicReference<String>();
  doAnswer(call->{var c=challenge();var result=(Map<?,?>)service.recover("owner@example.test",password,c.get("id"),answer(c),"recovery");recovered.set(result.get("token").toString());throw new IllegalStateException("Confirmation lost after recovery");}).when(remote).exchange(anyString(),anyString(),argThat(m->m.get("action").equals("ATTEST")));
  service.prepare(id);var r=rows.findById(id).orElseThrow();assertThat(r.status).isEqualTo("RETRYABLE");assertThat(r.leaseUntil).isNull();assertThat(((Map<?,?>)service.status(id,recovered.get(),"new")).get("retryAvailable")).isEqualTo(true);assertThat(slots.findById(r.slotId).orElseThrow().state).isEqualTo("RESERVED");assertThat(installed).hasSize(1);
 }
}
