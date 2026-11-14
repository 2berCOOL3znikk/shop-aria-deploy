package com.aria.service;
import com.aria.entity.*;
import com.aria.enums.Role;
import com.aria.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

/** A bootstrap key can assign one new empty slot. It can never reset an installed owner's password or mint JWTs. */
@Service
public class WorkspaceSlotService {
 private final boolean enabled;
 private final String binding,key;
 private final WorkspaceSlotInstallationRepository installations;
 private final AppUserRepository users;
 private final JdbcTemplate jdbc;
 private final AuditService audit;
 private final TransactionTemplate tx;
 private final LoginAttemptService attempts;
 private final tools.jackson.databind.ObjectMapper json=tools.jackson.databind.json.JsonMapper.builder().build();
 public boolean authorizeGatewayHeader(String proof){return enabled&&CloudWorkspaceProof.equal(proof,CloudWorkspaceProof.sign(key,"access:"+java.time.LocalDate.now(java.time.ZoneOffset.UTC)));}
 public WorkspaceSlotService(Environment env,WorkspaceSlotInstallationRepository installations,AppUserRepository users,JdbcTemplate jdbc,AuditService audit,PlatformTransactionManager manager,LoginAttemptService attempts){
  enabled=env.getProperty("aria.saas.slot-enabled",Boolean.class,false);binding=env.getProperty("aria.organization.key","demo");key=env.getProperty("aria.saas.slot-key","");
  if(enabled&&(!binding.matches("business-[a-f0-9]{32}")||!key.matches("[a-f0-9]{64}")))throw new IllegalStateException("Invalid isolated workspace slot");
  this.installations=installations;this.users=users;this.jdbc=jdbc;this.audit=audit;this.tx=new TransactionTemplate(manager);this.attempts=attempts;
 }
 private void boundary(){
  if(!enabled)throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  if(!binding.equals(jdbc.queryForObject("select organization_key from saas_organization where id=1",String.class)))throw new IllegalStateException("Slot/database binding mismatch");
  var role=jdbc.queryForMap("SELECT rolsuper,rolcreatedb,rolcreaterole, EXISTS(SELECT 1 FROM pg_auth_members m WHERE m.member=r.oid) AS member_of_another_role, EXISTS(SELECT 1 FROM pg_database d WHERE d.datname=current_database() AND d.datdba=r.oid) AS owns_database FROM pg_roles r WHERE rolname=current_user");
  if(Boolean.TRUE.equals(role.get("rolsuper"))||Boolean.TRUE.equals(role.get("rolcreatedb"))||Boolean.TRUE.equals(role.get("rolcreaterole"))||Boolean.TRUE.equals(role.get("member_of_another_role"))||!Boolean.TRUE.equals(role.get("owns_database")))throw new IllegalStateException("Slot requires an unprivileged database owner");
 }
 @jakarta.annotation.PostConstruct public void validate(){if(enabled){boundary();var s=installations.findById(1).orElseThrow(()->new IllegalStateException("New slot marker is missing"));if(!binding.equals(s.binding))throw new IllegalStateException("Slot marker mismatch");}}
 public Map<String,Object> handle(String raw,String proof,String address){
  if(!enabled)throw new ResponseStatusException(HttpStatus.NOT_FOUND);String limit="slot-bootstrap:"+address;attempts.check(limit);
  if(raw==null||raw.getBytes(StandardCharsets.UTF_8).length>16384||!CloudWorkspaceProof.equal(proof,CloudWorkspaceProof.sign(key,"request:"+raw))){attempts.failed(limit);throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Готовность кабинета не подтверждена");}
  try{
   Map<?,?> input=json.readValue(raw,Map.class);String action=text(input,"action"),nonce=text(input,"nonce");
   if(!Set.of("INSPECT","INSTALL","ATTEST").contains(action)||!nonce.matches("[a-f0-9]{32}")||!binding.equals(text(input,"binding"))||Math.abs(Duration.between(Instant.parse(text(input,"issuedAt")),Instant.now()).getSeconds())>180)throw new IllegalArgumentException("Invalid workspace request");
   Map<String,Object> result=tx.execute(status->{boundary();var s=installations.lockSingleton().orElseThrow();if(!binding.equals(s.binding))throw new IllegalStateException("Slot marker mismatch");if(action.equals("INSTALL"))install(s,input);return describe(s,nonce);});
   attempts.succeeded(limit);String encoded=Base64.getUrlEncoder().withoutPadding().encodeToString(json.writeValueAsBytes(result));return Map.of("data",encoded,"proof",CloudWorkspaceProof.sign(key,"response:"+encoded));
  }catch(ResponseStatusException e){throw e;}catch(IllegalArgumentException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Проверьте параметры подготовки кабинета");}catch(Exception e){throw new ResponseStatusException(HttpStatus.CONFLICT,"Кабинет не подтвердил безопасную подготовку");}
 }
 private void install(WorkspaceSlotInstallation s,Map<?,?> in){
  String id=text(in,"registrationId"),email=AuthService.normalizeEmail(text(in,"email")),company=text(in,"companyName").trim(),password=text(in,"passwordHash");
  if(!id.matches("[a-f0-9-]{36}")||!UUID.fromString(id).toString().equals(id)||company.isBlank()||company.length()>200||!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")||!password.matches("\\$2[aby]\\$[0-9]{2}\\$[./A-Za-z0-9]{53}"))throw new IllegalArgumentException();
  int cost=Integer.parseInt(password.substring(4,6));if(cost<10||cost>14)throw new IllegalArgumentException("Unsafe password hash cost");
  String emailHash=CompanySignupService.hash(email),companyHash=CompanySignupService.hash(company),credentialHash=CompanySignupService.hash(password);
  if(s.registrationId!=null){if(!s.registrationId.equals(id)||!s.emailHash.equals(emailHash)||!s.companyHash.equals(companyHash)||!s.credentialHash.equals(credentialHash))throw new ResponseStatusException(HttpStatus.CONFLICT,"Кабинет уже принадлежит другой регистрации");return;}
  if(users.count()!=0||!Boolean.TRUE.equals(jdbc.queryForObject("select not exists(select 1 from products) and not exists(select 1 from orders) and not exists(select 1 from connected_stores) and not exists(select 1 from company_registration_requests)",Boolean.class)))throw new ResponseStatusException(HttpStatus.CONFLICT,"Кабинет должен быть новым и пустым");
  var owner=new AppUser(email,password,company,Role.ADMIN);users.saveAndFlush(owner);jdbc.update("update saas_organization set name=?,demonstration=false where id=1",company);
  s.registrationId=id;s.emailHash=emailHash;s.companyHash=companyHash;s.credentialHash=credentialHash;s.ownerUserId=owner.getId();s.installedAt=LocalDateTime.now();installations.flush();audit.record("CLOUD_WORKSPACE_CREATED","workspace:"+id,"Владелец создал отдельный пустой кабинет; подтверждение email и внешние AI-согласия не заявляются");
 }
 private Map<String,Object> describe(WorkspaceSlotInstallation s,String nonce){
  boolean ready=s.registrationId!=null;
  if(ready){var user=users.findById(s.ownerUserId).orElseThrow();if(user.getRole()!=Role.ADMIN||!CompanySignupService.hash(user.getEmail()).equals(s.emailHash))throw new IllegalStateException("Workspace owner mismatch");}
  var r=new LinkedHashMap<String,Object>();r.put("binding",binding);r.put("registrationId",s.registrationId);r.put("emailHash",s.emailHash);r.put("companyHash",s.companyHash);r.put("ready",ready);r.put("empty",!ready&&users.count()==0);r.put("nonce",nonce);return r;
 }
 private String text(Map<?,?> map,String key){Object value=map.get(key);if(!(value instanceof String s))throw new IllegalArgumentException();return s;}
}
