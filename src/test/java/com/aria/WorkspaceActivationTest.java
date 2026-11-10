package com.aria;

import com.aria.entity.*;
import com.aria.enums.Role;
import com.aria.repository.*;
import com.aria.service.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Unit checks of the activation protocol; real role/database/process checks are in activation-e2e.cjs. */
class WorkspaceActivationTest {
 private final String id="759251f1-6bf0-465c-a0bd-447ca63f1269",binding="business-759251f16bf0465ca0bd447ca63f1269",code="1a".repeat(32),key="2b".repeat(32),email="owner@example.test";
 private WorkspaceActivationRepository markers;
 private AppUserRepository users;
 private JdbcTemplate jdbc;
 private AuditService audit;
 private WorkspaceActivation state;
 private AppUser owner;
 private MockEnvironment env;
 private WorkspaceActivationService service;
 private final BCryptPasswordEncoder passwords=new BCryptPasswordEncoder(4);
 @BeforeEach void before(){
  markers=mock(WorkspaceActivationRepository.class);users=mock(AppUserRepository.class);jdbc=mock(JdbcTemplate.class);audit=mock(AuditService.class);
  state=new WorkspaceActivation();state.requestId=id;state.binding=binding;state.accessHash=CompanySignupService.hash(code);state.probeHash=CompanySignupService.hash(key);state.ownerEmailHash=CompanySignupService.hash(email);state.ownerUserId=7L;state.deadline=Instant.now().plusSeconds(3600).truncatedTo(ChronoUnit.SECONDS);
  owner=new AppUser(email,passwords.encode("Bootstrap-private-password"),"Educational owner",Role.ADMIN);owner.setId(7L);
  when(markers.lockSingleton()).thenReturn(Optional.of(state));when(markers.findById(1L)).thenReturn(Optional.of(state));when(users.findByEmailIgnoreCase(email)).thenReturn(Optional.of(owner));when(users.lockByEmail(email)).thenReturn(Optional.of(owner));when(users.count()).thenReturn(1L);
  when(jdbc.queryForMap(anyString())).thenReturn(Map.of("rolsuper",false,"rolcreatedb",false,"rolcreaterole",false,"owns_database",true));
  when(jdbc.queryForObject(anyString(),eq(String.class))).thenAnswer(call->call.getArgument(0,String.class).contains("SELECT name")?"Educational company":binding);
  when(jdbc.queryForObject(anyString(),eq(Boolean.class))).thenReturn(true);
  env=new MockEnvironment().withProperty("aria.company-activation.enabled","true").withProperty("aria.organization.key",binding).withProperty("aria.admin.email",email).withProperty("aria.company-activation.request-id",id).withProperty("aria.company-activation.access-hash",state.accessHash).withProperty("aria.company-activation.probe-key",key).withProperty("aria.company-activation.deadline",state.deadline.toString());
  service=build();service.validateInstallation();
 }
 private WorkspaceActivationService build(){var tx=mock(PlatformTransactionManager.class);when(tx.getTransaction(any())).thenReturn(new SimpleTransactionStatus());return new WorkspaceActivationService(env,markers,users,passwords,jdbc,audit,tx,new LoginAttemptService());}
 @Test void failsClosedBeforeMarkerVerification(){assertThat(build().requiresActivation()).isTrue();when(markers.lockSingleton()).thenReturn(Optional.empty());assertThatThrownBy(()->build().validateInstallation()).isInstanceOf(IllegalStateException.class);}
 @Test void refusesPrivilegedRoleAndWrongDatabaseBinding(){when(jdbc.queryForMap(anyString())).thenReturn(Map.of("rolsuper",true,"rolcreatedb",false,"rolcreaterole",false,"owns_database",true));assertThatThrownBy(()->build().validateInstallation()).isInstanceOf(IllegalStateException.class).hasMessageContaining("unprivileged");when(jdbc.queryForMap(anyString())).thenReturn(Map.of("rolsuper",false,"rolcreatedb",false,"rolcreaterole",false,"owns_database",true));state.binding="other-company";assertThatThrownBy(()->build().validateInstallation()).isInstanceOf(IllegalStateException.class).hasMessageContaining("binding");}
 @Test void refusesUnclaimedPopulatedWorkspaceButAllowsRestartAfterClaim(){when(jdbc.queryForObject(anyString(),eq(Boolean.class))).thenReturn(false);assertThatThrownBy(()->build().validateInstallation()).isInstanceOf(IllegalStateException.class).hasMessageContaining("empty");state.claimedAt=Instant.now();build().validateInstallation();}
 @Test void rejectsMissingCodeWrongApplicationAndLimitsBadCodes(){assertThatThrownBy(()->service.info(id,null,"anonymous")).hasMessageContaining("404");assertThatThrownBy(()->service.info(UUID.randomUUID().toString(),code,"wrong-id")).hasMessageContaining("404");for(int i=0;i<6;i++)assertThatThrownBy(()->service.info(id,"ff".repeat(32),"one-client")).hasMessageContaining("404");assertThatThrownBy(()->service.info(id,code,"one-client")).hasMessageContaining("429");verify(users,never()).lockByEmail(anyString());}
 @Test void infoMasksContactAndNeverReturnsPasswordOrSecrets(){assertThat(service.info(id,code,"owner")).containsEntry("claimed",false).containsEntry("email","o•••@example.test").doesNotContainKeys("password","probeKey","accessHash","token","ownerEmail");}
 @Test void requiresExplicitReviewAndStrongPassword(){assertThatThrownBy(()->service.claim(id,code,"Strong-private-password",false,"owner")).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->service.claim(id,code,"short",true,"owner")).isInstanceOf(IllegalArgumentException.class);assertThat(owner.getTokenVersion()).isZero();assertThat(state.claimedAt).isNull();}
 @Test void claimHashesPasswordRevokesTokensAndCannotBeReplayed(){assertThat(service.requiresActivation()).isTrue();assertThat(service.claim(id,code,"Owner-private-password-123",true,"owner")).containsEntry("activated",true);assertThat(passwords.matches("Owner-private-password-123",owner.getPassword())).isTrue();assertThat(owner.getPassword()).doesNotContain("Owner-private");assertThat(owner.getTokenVersion()).isEqualTo(1);assertThat(service.requiresActivation()).isFalse();assertThatThrownBy(()->service.claim(id,code,"Other-private-password-123",true,"owner")).hasMessageContaining("409");assertThat(passwords.matches("Owner-private-password-123",owner.getPassword())).isTrue();verify(audit).record(eq("WORKSPACE_OWNER_ACTIVATED"),eq("user:7"),anyString());}
 @Test void expiredPendingClaimFailsWithoutChangingPassword(){env.setProperty("aria.company-activation.deadline",Instant.now().minusSeconds(60).toString());state.deadline=Instant.parse(env.getProperty("aria.company-activation.deadline"));var expired=build();expired.validateInstallation();assertThatThrownBy(()->expired.claim(id,code,"Owner-private-password-123",true,"owner")).hasMessageContaining("410");assertThat(state.claimedAt).isNull();assertThat(owner.getTokenVersion()).isZero();}
 @Test void signedAttestationIsBoundToNonceRequestAndWorkspace(){String nonce="ab".repeat(16);assertThatThrownBy(()->service.attest(key,id,nonce)).hasMessageContaining("403");var response=service.attest(WorkspaceProof.requestProof(key,id,nonce),id,nonce);assertThat(response).containsEntry("requestId",id).containsEntry("nonce",nonce).doesNotContainKeys("probeKey","ownerEmail","password","accessHash");assertThat(response.get("signature")).isEqualTo(WorkspaceProof.sign(key,id,binding,state.ownerEmailHash,nonce,state.deadline,null));assertThatThrownBy(()->service.attest(WorkspaceProof.requestProof(key,id,nonce),id,"cd".repeat(16))).hasMessageContaining("403");assertThat(WorkspaceProof.sign(key,id,binding,state.ownerEmailHash,nonce,state.deadline,null)).isNotEqualTo(WorkspaceProof.sign(key,id,"other-company",state.ownerEmailHash,nonce,state.deadline,null));}
 @Test void disabledActivationDoesNotReadDatabase(){env.setProperty("aria.company-activation.enabled","false");var disabled=build();clearInvocations(markers,jdbc);disabled.validateInstallation();assertThat(disabled.requiresActivation()).isFalse();verifyNoInteractions(markers,jdbc);}
}
