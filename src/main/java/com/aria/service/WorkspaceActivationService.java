package com.aria.service;

import com.aria.entity.WorkspaceActivation;
import com.aria.enums.Role;
import com.aria.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

/** Login stays closed until an operator-installed marker has been checked and the owner has claimed it. */
@Service
public class WorkspaceActivationService {
 private final boolean enabled;
 private final String request,binding,email,accessHash,probeKey;
 private final Instant deadline;
 private final WorkspaceActivationRepository activations;
 private final AppUserRepository users;
 private final PasswordEncoder passwords;
 private final JdbcTemplate jdbc;
 private final AuditService audit;
 private final TransactionTemplate transaction;
 private final LoginAttemptService attempts;
 private volatile boolean verified;
 public WorkspaceActivationService(Environment env,WorkspaceActivationRepository activations,AppUserRepository users,PasswordEncoder passwords,JdbcTemplate jdbc,AuditService audit,PlatformTransactionManager manager,LoginAttemptService attempts){
  enabled=env.getProperty("aria.company-activation.enabled",Boolean.class,false);binding=env.getProperty("aria.organization.key","demo");email=AuthService.normalizeEmail(env.getProperty("aria.admin.email","admin@aria.shop"));request=env.getProperty("aria.company-activation.request-id","");accessHash=env.getProperty("aria.company-activation.access-hash","");probeKey=env.getProperty("aria.company-activation.probe-key","");
  this.activations=activations;this.users=users;this.passwords=passwords;this.jdbc=jdbc;this.audit=audit;this.attempts=attempts;transaction=new TransactionTemplate(manager);
  if(enabled){
   if(Set.of("demo","aria-registration").contains(binding)||!binding.matches("[a-z0-9][a-z0-9-]{0,79}")||!request.matches("[a-f0-9-]{36}")||!accessHash.matches("[a-f0-9]{64}")||!probeKey.matches("[a-f0-9]{64}"))throw new IllegalStateException("Invalid new-workspace activation configuration");
   try{if(!UUID.fromString(request).toString().equals(request))throw new Exception();deadline=Instant.parse(env.getRequiredProperty("aria.company-activation.deadline"));}catch(Exception e){throw new IllegalStateException("Invalid new-workspace activation deadline");}
  }else deadline=null;
 }
 public boolean enabled(){return enabled;}
 @jakarta.annotation.PostConstruct
 public void validateInstallation(){
  if(!enabled)return;
  transaction.executeWithoutResult(tx->{
   var state=activations.lockSingleton().orElseThrow(()->new IllegalStateException("An operator must install activation in a newly created company database"));
   var role=jdbc.queryForMap("SELECT rolsuper,rolcreatedb,rolcreaterole, EXISTS(SELECT 1 FROM pg_database WHERE datname=current_database() AND datdba=pg_roles.oid) AS owns_database FROM pg_roles WHERE rolname=current_user");
   if(Boolean.TRUE.equals(role.get("rolsuper"))||Boolean.TRUE.equals(role.get("rolcreatedb"))||Boolean.TRUE.equals(role.get("rolcreaterole"))||!Boolean.TRUE.equals(role.get("owns_database")))throw new IllegalStateException("Use a dedicated, unprivileged company database owner");
   if(!state.requestId.equals(request)||!state.binding.equals(binding)||!state.ownerEmailHash.equals(CompanySignupService.hash(email))||!state.accessHash.equals(accessHash)||!state.probeHash.equals(CompanySignupService.hash(probeKey))||!state.deadline.equals(deadline)||!binding.equals(jdbc.queryForObject("SELECT organization_key FROM saas_organization WHERE id=1",String.class)))throw new IllegalStateException("Activation/database binding mismatch; refusing startup");
   var owner=users.findByEmailIgnoreCase(email).orElseThrow(()->new IllegalStateException("Owner account is missing"));if(!owner.getId().equals(state.ownerUserId)||owner.getRole()!=Role.ADMIN)throw new IllegalStateException("Activation owner does not match the prepared account");
   if(state.claimedAt==null){
    if(users.count()!=1||!Boolean.TRUE.equals(jdbc.queryForObject("SELECT NOT EXISTS(SELECT 1 FROM products) AND NOT EXISTS(SELECT 1 FROM orders) AND NOT EXISTS(SELECT 1 FROM connected_stores) AND NOT EXISTS(SELECT 1 FROM company_registration_requests)",Boolean.class)))throw new IllegalStateException("An unclaimed workspace must be empty");
    if(deadline.isAfter(Instant.now().plus(Duration.ofDays(7))))throw new IllegalStateException("Activation cannot last more than seven days");
   }
  });verified=true;
 }
 public boolean requiresActivation(){if(!enabled)return false;if(!verified)return true;return Boolean.TRUE.equals(transaction.execute(tx->activations.findById(1L).map(s->s.claimedAt==null).orElse(true)));}
 public void requireOwnerCode(String id,String code){
  requireOperational();if(!request.equals(id)||code==null||!code.matches("[a-f0-9]{64}")||!WorkspaceProof.equal(accessHash,CompanySignupService.hash(code)))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Код активации не найден или неверен");
  if(!deadline.isAfter(Instant.now()))throw new ResponseStatusException(HttpStatus.GONE,"Срок активации истёк. Обратитесь к оператору");
 }
 private void requireOperational(){if(!enabled)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Активация кабинета здесь не включена");if(!verified)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Кабинет завершает подготовку. Повторите проверку позже");}
 private String guardedCode(String id,String code,String address,String operation){requireOperational();String limit="workspace-"+operation+":"+address;attempts.check(limit);try{requireOwnerCode(id,code);}catch(ResponseStatusException error){attempts.failed(limit);throw error;}return limit;}
 public Map<String,Object> info(String id,String code,String address){String limit=guardedCode(id,code,address,"read");attempts.succeeded(limit);return transaction.execute(tx->{var s=activations.findById(1L).orElseThrow();var result=new LinkedHashMap<String,Object>();result.put("requestId",request);result.put("claimed",s.claimedAt!=null);result.put("expiresAt",deadline);result.put("companyName",jdbc.queryForObject("SELECT name FROM saas_organization WHERE id=1",String.class));result.put("email",email.charAt(0)+"•••"+email.substring(email.indexOf('@')));return result;});}
 public Map<String,Object> attest(String signature,String id,String nonce){
  requireOperational();if(!request.equals(id)||nonce==null||!nonce.matches("[a-f0-9]{32}")||!WorkspaceProof.equal(signature,WorkspaceProof.requestProof(probeKey,id,nonce)))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Готовность кабинета не подтверждена");
  return transaction.execute(tx->{var state=activations.findById(1L).orElseThrow();if(state.claimedAt==null&&!deadline.isAfter(Instant.now()))throw new ResponseStatusException(HttpStatus.GONE,"Срок активации истёк. Обратитесь к оператору");var r=new LinkedHashMap<String,Object>();r.put("requestId",request);r.put("binding",binding);r.put("ownerHash",state.ownerEmailHash);r.put("nonce",nonce);r.put("expiresAt",deadline);r.put("claimedAt",state.claimedAt);r.put("signature",WorkspaceProof.sign(probeKey,request,binding,state.ownerEmailHash,nonce,deadline,state.claimedAt));return r;});
 }
 public Map<String,Object> claim(String id,String code,String password,boolean reviewed,String address){
  String limit=guardedCode(id,code,address,"claim");
  if(!reviewed)throw new IllegalArgumentException("Подтвердите создание доступа владельца");AuthService.validatePassword(password);
  return transaction.execute(tx->{var state=activations.lockSingleton().orElseThrow();if(state.claimedAt!=null)throw new ResponseStatusException(HttpStatus.CONFLICT,"Кабинет уже активирован. Войдите с выбранным паролем");if(!deadline.isAfter(Instant.now()))throw new ResponseStatusException(HttpStatus.GONE,"Срок активации истёк. Обратитесь к оператору");
   var owner=users.lockByEmail(email).orElseThrow();if(!owner.getId().equals(state.ownerUserId)||owner.getRole()!=Role.ADMIN)throw new IllegalStateException("Учётная запись владельца изменилась");String encoded=passwords.encode(password);if(!deadline.isAfter(Instant.now()))throw new ResponseStatusException(HttpStatus.GONE,"Срок активации истёк. Обратитесь к оператору");owner.setPassword(encoded);owner.revokeTokens();users.saveAndFlush(owner);state.claimedAt=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);activations.flush();attempts.succeeded(limit);audit.record("WORKSPACE_OWNER_ACTIVATED","user:"+owner.getId(),"Владелец подтвердил одноразовый код и выбрал пароль; прежние токены отозваны");return Map.of("activated",true,"loginPath","/login.html");});
 }
}
