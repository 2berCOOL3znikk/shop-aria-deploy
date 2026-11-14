package com.aria.service;
import com.aria.entity.*;
import com.aria.repository.*;
import com.aria.integration.IntegrationVault;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** Durable admission into pre-provisioned isolated processes. No provider administration credential lives here. */
@Service
public class CloudRegistrationService implements org.springframework.beans.factory.DisposableBean {
 private final boolean enabled,postgres,autoPrepare;
 private final String fingerprintKey;
 private final CloudRegistrationRepository rows;
 private final WorkspacePoolSlotRepository slots;
 private final CompanyRegistrationRequestRepository locks;
 private final IntegrationVault vault;
 private final CloudWorkspaceTransport remote;
 private final CaptchaService captcha;
 private final CompanySignupService intake;
 private final PasswordEncoder passwords;
 private final AuditService audit;
 private final TransactionTemplate tx;
 @org.springframework.beans.factory.annotation.Autowired private AppUserRepository nativeUsers;
 private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"aria-workspace-preparation");t.setDaemon(true);return t;});
 private final Set<String> queued=ConcurrentHashMap.newKeySet();
 public CloudRegistrationService(Environment env,CloudRegistrationRepository rows,WorkspacePoolSlotRepository slots,CompanyRegistrationRequestRepository locks,IntegrationVault vault,CloudWorkspaceTransport remote,CaptchaService captcha,CompanySignupService intake,PasswordEncoder passwords,AuditService audit,PlatformTransactionManager manager){
  enabled=env.getProperty("aria.saas.gateway-enabled",Boolean.class,false);autoPrepare=env.getProperty("aria.saas.auto-prepare",Boolean.class,true);postgres="org.postgresql.Driver".equals(env.getProperty("spring.datasource.driver-class-name",env.getProperty("spring.datasource.driverClassName","org.postgresql.Driver")));fingerprintKey=env.getProperty("aria.integrations.encryption-key","");
  if(enabled&&(!"aria-v4-staging".equals(env.getProperty("aria.organization.key"))||!vault.available()||!intake.available()))throw new IllegalStateException("Cloud admission requires the isolated staging coordinator and its own encryption key");
  this.rows=rows;this.slots=slots;this.locks=locks;this.vault=vault;this.remote=remote;this.captcha=captcha;this.intake=intake;this.passwords=passwords;this.audit=audit;tx=new TransactionTemplate(manager);
 }
 public boolean enabled(){return enabled;}
 private void require(){if(!enabled)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Регистрация кабинетов здесь не включена");}
 private void serialize(){if(postgres)locks.serializeCreation("saas-workspace-registry");}
 public Object capabilities(){return Map.of("enabled",enabled,"available",enabled?slots.countByState("AVAILABLE"):0,"emailAutomaticallyVerified",false,"limitedFreeCapacity",true);}
 public Object challenge(String address){require();return intake.challenge("saas:"+address);}
 public record Signup(String id,String token,String admissionCode,String email,String password,String companyName,boolean processingConsent,boolean authorityConfirmed,String captchaId,String captchaAnswer){@Override public String toString(){return "CloudSignup[redacted]";}}
 public Object register(Signup in,String address){
  return register(in,address,null);
 }
 public Object register(Signup in,String address,String authenticatedEmail){
  require();intake.rate("saas:"+address,30,"saas-attempt");validateReceipt(in.id,in.token);String email=AuthService.normalizeEmail(in.email),company=in.companyName.trim();
  if(company.isBlank()||company.length()>200||!in.processingConsent||!in.authorityConfirmed||in.password.length()<12)throw new IllegalArgumentException("Проверьте компанию, пароль и отдельные согласия");AuthService.validatePassword(in.password);
  String fingerprint=CloudWorkspaceProof.sign(fingerprintKey,"registration-v1:"+java.util.stream.Stream.of(in.id,email,company,in.password,Objects.toString(in.admissionCode,"" )).map(v->v.length()+":"+v).collect(java.util.stream.Collectors.joining()));
  if(nativeUsers!=null&&nativeUsers.existsByEmailIgnoreCase(email)&&!email.equals(authenticatedEmail))throw conflict("Этот email уже используется. Войдите в кабинет предпросмотра или выберите другой email");
  var view=tx.execute(status->{serialize();var existing=rows.lockById(in.id);if(existing.isPresent()){var r=existing.get();authorize(r,in.token);if(!CloudWorkspaceProof.equal(fingerprint,r.fingerprint))throw conflict("Данные этой регистрации изменились. Откройте сохранённый статус");return view(r);}
   intake.rate("saas:"+address,10,"saas-register");if(!captcha.verifyAnswer(in.captchaId,in.captchaAnswer))throw new IllegalArgumentException("Пройдите проверку регистрации заново");
   if(in.admissionCode==null||!in.admissionCode.matches("[a-f0-9]{64}"))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Для бесплатного пилота нужно приглашение");var admission=slots.findByAdmissionHash(CompanySignupService.hash(in.admissionCode)).orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN,"Приглашение недоступно"));if(!admission.state.equals("AVAILABLE"))throw conflict("Приглашение уже используется. Войдите или восстановите подготовку");
   if(rows.existsByAccessHash(CompanySignupService.hash(in.token)))throw conflict("Код уже связан с другой регистрацией");if(rows.findByEmailHash(CompanySignupService.hash(email)).isPresent())throw conflict("Для этого email уже есть регистрация. Войдите или восстановите подготовку");if(rows.count()>=1000)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Приём регистраций временно приостановлен");
   var r=new CloudRegistration();r.id=in.id;r.accessHash=CompanySignupService.hash(in.token);r.email=email;r.emailHash=CompanySignupService.hash(email);r.companyName=company;r.fingerprint=fingerprint;r.credentialCipher=vault.encrypt(passwords.encode(in.password));r.status="WAITING_CAPACITY";r.processingConsent=true;r.authorityConfirmed=true;r.createdAt=LocalDateTime.now();r.updatedAt=r.createdAt;r=rows.saveAndFlush(r);var admitted=slots.lockById(admission.id).orElseThrow();if(!admitted.state.equals("AVAILABLE"))throw conflict("Приглашение уже используется");admitted.state="RESERVED";admitted.registrationId=r.id;r.slotId=admitted.id;r.status="PREPARING";rows.flush();audit.record("CLOUD_REGISTRATION_RECEIVED","registration:"+r.id,"Сохранены отдельные согласия и защищённый хеш пароля; кабинет ещё не заявлен готовым");return view(r);
  });queue(in.id,0);return view;
 }
 private void reserve(CloudRegistration r){if(r.slotId==null)r.status="WAITING_CAPACITY";}
 public Object status(String id,String code,String address){require();intake.rate("saas:"+address,180,"saas-status");validateReceipt(id,code);return tx.execute(s->{var r=rows.findById(id).orElseThrow(this::missing);authorize(r,code);return view(r);});}
 public Object retry(String id,String code,String address){require();intake.rate("saas:"+address,15,"saas-retry");validateReceipt(id,code);var result=tx.execute(s->{serialize();var r=rows.lockById(id).orElseThrow(this::missing);authorize(r,code);if(!r.status.equals("ACTIVE")&&(r.leaseUntil==null||r.leaseUntil.isBefore(LocalDateTime.now()))){reserve(r);r.status=r.slotId==null?"WAITING_CAPACITY":"PREPARING";r.attempts=0;r.failureCode=null;r.updatedAt=LocalDateTime.now();}return view(r);});queue(id,0);return result;}
 public Object recover(String email,String password,String challengeId,String answer,String address){
  require();intake.rate("saas:"+address,8,"saas-recover");if(!captcha.verifyAnswer(challengeId,answer))throw new IllegalArgumentException("Пройдите проверку регистрации заново");String normalized=AuthService.normalizeEmail(email),code=CloudWorkspaceProof.random();
  var result=tx.execute(s->{serialize();var found=rows.findByEmailHash(CompanySignupService.hash(normalized));if(found.isEmpty())throw missing();var r=rows.lockById(found.get().id).orElseThrow(this::missing);if(r.status.equals("ACTIVE"))throw conflict("Кабинет уже готов. Войдите по email и своему паролю");if(r.credentialCipher==null||!passwords.matches(password,vault.decrypt(r.credentialCipher)))throw missing();r.accessHash=CompanySignupService.hash(code);var v=view(r);v.put("token",code);return v;});return result;
 }
 private Map<String,Object> view(CloudRegistration r){var v=new LinkedHashMap<String,Object>();v.put("id",r.id);v.put("companyName",r.companyName);v.put("email",r.email.charAt(0)+"•••"+r.email.substring(r.email.indexOf('@')));v.put("status",r.status);v.put("workspaceReady",r.status.equals("ACTIVE"));v.put("emailAutomaticallyVerified",false);v.put("retryAvailable",r.status.equals("RETRYABLE")||r.status.equals("WAITING_CAPACITY"));v.put("failureCode",r.failureCode);v.put("createdAt",r.createdAt);if(r.status.equals("ACTIVE"))v.put("loginPath","/login.html?fresh=1");return v;}
 private void validateReceipt(String id,String code){try{if(!UUID.fromString(id).toString().equals(id)||code==null||!code.matches("[a-f0-9]{64}"))throw new Exception();}catch(Exception e){throw missing();}}
 private void authorize(CloudRegistration r,String code){if(!CloudWorkspaceProof.equal(r.accessHash,CompanySignupService.hash(code)))throw missing();}
 private ResponseStatusException missing(){return new ResponseStatusException(HttpStatus.NOT_FOUND,"Регистрация или код доступа не найдены");}
 private ResponseStatusException conflict(String text){return new ResponseStatusException(HttpStatus.CONFLICT,text);}
 public Map<String,Object> request(String action,String binding){var r=new LinkedHashMap<String,Object>();r.put("action",action);r.put("binding",binding);r.put("nonce",UUID.randomUUID().toString().replace("-",""));r.put("issuedAt",Instant.now().toString());return r;}
 public Object addSlot(String id,String binding,String origin,String secret,String admissionCode){
  require();if(admissionCode==null||!admissionCode.matches("[a-f0-9]{64}")||!UUID.fromString(id).toString().equals(id)||!binding.matches("business-[a-f0-9]{32}")||!secret.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Проверьте новый кабинет");remote.validateOrigin(origin);var checked=remote.exchange(origin,secret,request("INSPECT",binding));if(!binding.equals(checked.get("binding"))||!Boolean.TRUE.equals(checked.get("empty"))||Boolean.TRUE.equals(checked.get("ready")))throw conflict("Принимаются только новые пустые кабинеты");
  Object result=tx.execute(s->{serialize();var existing=slots.findByBinding(binding);if(existing.isPresent()){var old=existing.get();if(!old.origin.equals(origin)||!old.state.equals("AVAILABLE")||!CloudWorkspaceProof.equal(vault.decrypt(old.secretCipher),secret)||!CompanySignupService.hash(admissionCode).equals(old.admissionHash))throw conflict("Кабинет уже используется");return slotView(old);}if(slots.count()>=2)throw conflict("Достигнут лимит бесплатного пилота");var slot=new WorkspacePoolSlot();slot.id=id;slot.binding=binding;slot.origin=origin;slot.secretCipher=vault.encrypt(secret);slot.admissionHash=CompanySignupService.hash(admissionCode);slot.state="AVAILABLE";slot.createdAt=LocalDateTime.now();slots.saveAndFlush(slot);audit.record("CLOUD_EMPTY_SLOT_VERIFIED","pool-slot:"+id,"Подписанная готовность отдельного пустого процесса проверена; данные других компаний не копировались");return slotView(slot);});resumePending();return result;
 }
 private Object slotView(WorkspacePoolSlot s){return Map.of("id",s.id,"state",s.state,"assigned",s.registrationId!=null);}
 public Object pool(){require();return Map.of("slots",slots.findAll().stream().map(this::slotView).toList());}
 @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
 public void resumePending(){if(enabled)rows.findTop10ByStatusNotOrderByCreatedAtAsc("ACTIVE").forEach(r->{if(!r.status.equals("RETRYABLE"))queue(r.id,0);});}
 private void queue(String id,int delay){if(!enabled||!autoPrepare||queued.size()>=8||!queued.add(id))return;worker.schedule(()->{try{prepare(id);}finally{queued.remove(id);var r=rows.findById(id);if(r.isPresent()&&r.get().status.equals("PREPARING")&&r.get().attempts<4)queue(id,10);}},delay,TimeUnit.SECONDS);}
 public void prepare(String id){
  record Work(String id,String slot,String binding,String origin,String keyCipher,String email,String company,String passwordCipher,int attempt,LocalDateTime lease){@Override public String toString(){return "Preparation[redacted]";}}
  Work w=tx.execute(t->{serialize();var optional=rows.lockById(id);if(optional.isEmpty())return null;var r=optional.get();if(r.status.equals("ACTIVE")||r.leaseUntil!=null&&r.leaseUntil.isAfter(LocalDateTime.now()))return null;reserve(r);if(r.slotId==null)return null;var slot=slots.lockById(r.slotId).orElseThrow();if(!"RESERVED".equals(slot.state)||!id.equals(slot.registrationId))throw new IllegalStateException("Allocation mismatch");r.status="PREPARING";r.attempts++;r.leaseUntil=LocalDateTime.now().plusSeconds(90).truncatedTo(java.time.temporal.ChronoUnit.MICROS);rows.flush();return new Work(r.id,slot.id,slot.binding,slot.origin,slot.secretCipher,r.email,r.companyName,r.credentialCipher,r.attempts,r.leaseUntil);});
  if(w==null)return;
  try{
   String key=vault.decrypt(w.keyCipher),password=vault.decrypt(w.passwordCipher);var input=request("INSTALL",w.binding);input.put("registrationId",w.id);input.put("email",w.email);input.put("companyName",w.company);input.put("passwordHash",password);remote.exchange(w.origin,key,input);
   var confirmed=remote.exchange(w.origin,key,request("ATTEST",w.binding));if(!w.binding.equals(confirmed.get("binding"))||!Boolean.TRUE.equals(confirmed.get("ready"))||!w.id.equals(confirmed.get("registrationId"))||!CompanySignupService.hash(w.email).equals(confirmed.get("emailHash"))||!CompanySignupService.hash(w.company).equals(confirmed.get("companyHash")))throw new IllegalStateException("Readback mismatch");
   // Recovering the status code does not invalidate independently verified ownership.
   tx.executeWithoutResult(t->{var r=rows.lockById(w.id).orElseThrow();if(r.status.equals("ACTIVE"))return;var slot=slots.lockById(w.slot).orElseThrow();if(!w.id.equals(slot.registrationId)||!w.slot.equals(r.slotId)||!w.binding.equals(slot.binding)||!w.email.equals(r.email)||!w.company.equals(r.companyName))throw new IllegalStateException("Allocation changed");r.status="ACTIVE";r.credentialCipher=null;r.leaseUntil=null;r.failureCode=null;r.updatedAt=LocalDateTime.now();slot.state="CLAIMED";rows.flush();audit.record("CLOUD_WORKSPACE_READY","registration:"+r.id,"Создание и независимое повторное чтение владельца/привязки подтверждены отдельным процессом");});
  }catch(Exception e){tx.executeWithoutResult(t->{var r=rows.lockById(w.id).orElseThrow();if(r.status.equals("ACTIVE")||r.attempts!=w.attempt||!Objects.equals(r.leaseUntil,w.lease))return;r.leaseUntil=null;r.status=r.attempts>=4?"RETRYABLE":"PREPARING";r.failureCode="WORKSPACE_NOT_CONFIRMED";r.updatedAt=LocalDateTime.now();});}
 }
 public Optional<WorkspacePoolSlot> loginSlot(String email){if(!enabled)return Optional.empty();var r=rows.findByEmailHash(CompanySignupService.hash(AuthService.normalizeEmail(email)));if(r.isEmpty())return Optional.empty();if(!r.get().status.equals("ACTIVE"))throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Кабинет готовится. Откройте статус регистрации");return activeSlot(r.get().slotId);}
 public Optional<WorkspacePoolSlot> activeSlot(String id){return slots.findById(id).filter(s->s.state.equals("CLAIMED")&&s.registrationId!=null&&rows.findById(s.registrationId).map(r->r.status.equals("ACTIVE")&&s.id.equals(r.slotId)).orElse(false));}
 public Optional<WorkspacePoolSlot> slotByBinding(String binding){if(!enabled)return Optional.empty();return slots.findByBinding(binding).flatMap(s->activeSlot(s.id));}
 public void validateOrigin(String origin){remote.validateOrigin(origin);}
 @Override public void destroy(){worker.shutdownNow();}
}
