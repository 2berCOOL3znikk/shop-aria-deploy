package com.aria;
import com.aria.entity.*;
import com.aria.repository.*;
import com.aria.service.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.*;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.*;
import java.time.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Protocol unit checks. PostgreSQL allocation and browser E2E are recorded separately. */
class WorkspaceSlotSecurityTest {
 private WorkspaceSlotService service;private WorkspaceSlotInstallation marker;private AppUser owner;
 private AppUserRepository users;private JdbcTemplate jdbc;private AuditService audit;
 private String secret="91".repeat(32),binding="business-"+"a".repeat(32),registration=UUID.randomUUID().toString();
 private tools.jackson.databind.ObjectMapper json=tools.jackson.databind.json.JsonMapper.builder().build();
 private String password=new BCryptPasswordEncoder().encode("Owner-password-12345");
 @BeforeEach void setup(){
  marker=new WorkspaceSlotInstallation();marker.binding=binding;var installations=mock(WorkspaceSlotInstallationRepository.class);when(installations.findById(1)).thenReturn(Optional.of(marker));when(installations.lockSingleton()).thenReturn(Optional.of(marker));
  users=mock(AppUserRepository.class);when(users.count()).thenAnswer(c->owner==null?0L:1L);when(users.saveAndFlush(any())).thenAnswer(c->{owner=c.getArgument(0);owner.setId(7L);return owner;});when(users.findById(7L)).thenAnswer(c->Optional.ofNullable(owner));
  jdbc=mock(JdbcTemplate.class);when(jdbc.queryForObject(anyString(),eq(String.class))).thenReturn(binding);when(jdbc.queryForObject(anyString(),eq(Boolean.class))).thenReturn(true);when(jdbc.queryForMap(anyString())).thenReturn(Map.of("rolsuper",false,"rolcreatedb",false,"rolcreaterole",false,"owns_database",true));audit=mock(AuditService.class);
  var manager=mock(PlatformTransactionManager.class);when(manager.getTransaction(any())).thenAnswer(c->new SimpleTransactionStatus());
  var env=new MockEnvironment().withProperty("aria.saas.slot-enabled","true").withProperty("aria.saas.slot-key",secret).withProperty("aria.organization.key",binding);
  service=new WorkspaceSlotService(env,installations,users,jdbc,audit,manager,new LoginAttemptService());service.validate();
 }
 private String payload(String action,String id)throws Exception{var r=new LinkedHashMap<String,Object>();r.put("action",action);r.put("binding",binding);r.put("nonce",UUID.randomUUID().toString().replace("-",""));r.put("issuedAt",Instant.now().toString());if(action.equals("INSTALL")){r.put("registrationId",id);r.put("email","owner@example.test");r.put("companyName","New company");r.put("passwordHash",password);}return json.writeValueAsString(r);}
 private Map<?,?> call(String action,String id)throws Exception{String raw=payload(action,id);var response=service.handle(raw,CloudWorkspaceProof.sign(secret,"request:"+raw),"test");String data=response.get("data").toString();assertThat(CloudWorkspaceProof.equal(response.get("proof").toString(),CloudWorkspaceProof.sign(secret,"response:"+data))).isTrue();return json.readValue(Base64.getUrlDecoder().decode(data),Map.class);}
 @Test void bootstrapHasSeparateServerAuthorizationAndSignedBodies()throws Exception{assertThat(service.authorizeGatewayHeader(null)).isFalse();assertThat(service.authorizeGatewayHeader(CloudWorkspaceProof.sign("wrong","access:"+LocalDate.now(ZoneOffset.UTC)))).isFalse();assertThat(service.authorizeGatewayHeader(CloudWorkspaceProof.sign(secret,"access:"+LocalDate.now(ZoneOffset.UTC)))).isTrue();String raw=payload("INSTALL",registration);assertThatThrownBy(()->service.handle(raw,"bad","test")).hasMessageContaining("403");verify(users,never()).saveAndFlush(any());}
 @Test void installationIsOneTimeAndCannotResetAnExistingOwnerPassword()throws Exception{assertThat(call("INSPECT",registration).get("empty")).isEqualTo(true);assertThat(call("INSTALL",registration).get("ready")).isEqualTo(true);String changed=new BCryptPasswordEncoder().encode("New-owner-password-456");owner.setPassword(changed);call("INSTALL",registration);assertThat(owner.getPassword()).isEqualTo(changed);verify(users,times(1)).saveAndFlush(any());var attest=call("ATTEST",registration);assertThat(attest.get("registrationId")).isEqualTo(registration);for(String key:List.of("password","passwordHash","credentialHash","token","secret"))assertThat(attest.containsKey(key)).isFalse();}
 @Test void anotherRegistrationCannotTakeAnAssignedSlot()throws Exception{call("INSTALL",registration);assertThatThrownBy(()->call("INSTALL",UUID.randomUUID().toString())).hasMessageContaining("409");assertThat(marker.registrationId).isEqualTo(registration);verify(users,times(1)).saveAndFlush(any());}
 @Test void nonemptyOrPrivilegedDatabaseIsRejected()throws Exception{when(jdbc.queryForMap(anyString())).thenReturn(Map.of("rolsuper",false,"rolcreatedb",true,"rolcreaterole",false,"owns_database",true));assertThatThrownBy(service::validate).isInstanceOf(IllegalStateException.class);when(jdbc.queryForMap(anyString())).thenReturn(Map.of("rolsuper",false,"rolcreatedb",false,"rolcreaterole",false,"owns_database",true));when(jdbc.queryForObject(anyString(),eq(Boolean.class))).thenReturn(false);assertThatThrownBy(()->call("INSTALL",registration)).hasMessageContaining("409");verify(users,never()).saveAndFlush(any());}
}
