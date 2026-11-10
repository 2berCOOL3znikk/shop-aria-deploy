package com.aria.service;

import com.aria.integration.IntegrationVault;
import com.aria.repository.CompanyRegistrationRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Local operator orchestration. HTTP returns metadata only; setup secrets never leave the servers. */
@Service
public class CompanyProvisioningService {
 private final CompanySignupService signup;
 private final CompanyRegistrationRequestRepository rows;
 private final IntegrationVault vault;
 private final LocalWorkspaceProbe probe;
 private final AuditService audit;
 private final TransactionTemplate transaction;
 public CompanyProvisioningService(CompanySignupService signup,CompanyRegistrationRequestRepository rows,IntegrationVault vault,LocalWorkspaceProbe probe,AuditService audit,PlatformTransactionManager manager){this.signup=signup;this.rows=rows;this.vault=vault;this.probe=probe;this.audit=audit;transaction=new TransactionTemplate(manager);}
 public Map<String,Object> prepare(String id,long version){
  signup.requireDesk();probe.requireLocal();
  return transaction.execute(tx->{var r=rows.lockById(id).orElseThrow(CompanyProvisioningService::missing);
   if(r.version!=version)throw changed();
   if(!Set.of("VERIFIED","PREPARING").contains(r.status))throw new ResponseStatusException(HttpStatus.CONFLICT,"Сначала завершите проверку заявки");
   if(r.status.equals("VERIFIED")){
    byte[] key=new byte[32];new java.security.SecureRandom().nextBytes(key);
    r.setupSecretCipher=vault.encrypt(HexFormat.of().formatHex(key));Arrays.fill(key,(byte)0);
    r.workspaceBinding="business-"+UUID.fromString(id).toString().replace("-","");r.activationDeadline=Instant.now().plus(Duration.ofDays(7)).truncatedTo(ChronoUnit.SECONDS);r.status="PREPARING";r.updatedAt=LocalDateTime.now();rows.flush();
    audit.record("COMPANY_WORKSPACE_PREPARED","company-request:"+id,"Создано локальное задание оператора; кабинет пока не подтверждён");
   }
   // The server-local CLI reads the encrypted key directly from the private database.
   // No email, key, password, access hash or downloadable plaintext setup is returned here.
   return Map.of("id",r.id,"status",r.status,"version",r.version,"serverSetupPrepared",true);
  });
 }
 public Map<String,Object> verify(String id,long version,String address){signup.requireDesk();probe.validate(address);return verifySnapshot(snapshot(id,version,null),address,false);}
 public Map<String,Object> refresh(String id,String token,String address){signup.requireDesk();signup.rate(address,30,"workspace-refresh");var s=snapshot(id,null,token);if(s.url==null)throw new ResponseStatusException(HttpStatus.CONFLICT,"Кабинет ещё не проверен оператором");return verifySnapshot(s,s.url,true);}
 private record Snapshot(String id,long version,String cipher,String binding,String ownerHash,Instant deadline,String url){@Override public String toString(){return "WorkspaceSnapshot[redacted]";}}
 private Snapshot snapshot(String id,Long version,String token){return transaction.execute(tx->{var r=rows.findById(id).orElseThrow(CompanyProvisioningService::missing);if(version==null)signup.requireAccess(r,token);if(version!=null&&r.version!=version)throw changed();if(!Set.of("PREPARING","READY","ACTIVE").contains(r.status)||r.setupSecretCipher==null)throw new ResponseStatusException(HttpStatus.CONFLICT,"Кабинет ещё не подготовлен");return new Snapshot(r.id,r.version,r.setupSecretCipher,r.workspaceBinding,CompanySignupService.hash(r.ownerEmail),r.activationDeadline,r.workspaceUrl);});}
 private Map<String,Object> verifySnapshot(Snapshot s,String address,boolean owner){
  probe.validate(address);
  var checked=probe.check(address,vault.decrypt(s.cipher),s.id,s.binding,s.ownerHash,s.deadline); // no DB lock is held during I/O
  return transaction.execute(tx->{var r=rows.lockById(s.id).orElseThrow(CompanyProvisioningService::missing);
   if(r.version!=s.version||!Objects.equals(r.setupSecretCipher,s.cipher))throw changed();
   if(r.workspaceActivatedAt!=null&&!Objects.equals(r.workspaceActivatedAt,checked.claimedAt()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Состояние активации изменилось. Обратитесь к оператору");
   r.workspaceUrl=address;r.workspaceCheckedAt=Instant.now().truncatedTo(ChronoUnit.MILLIS);r.workspaceActivatedAt=checked.claimedAt();r.status=checked.claimedAt()==null?"READY":"ACTIVE";r.updatedAt=LocalDateTime.now();rows.flush();
   audit.record("COMPANY_WORKSPACE_VERIFIED","company-request:"+s.id,r.status+"; подписанный ответ отдельного локального процесса проверен");return owner?signup.publicView(r):signup.operatorView(r);
  });
 }
 private static ResponseStatusException changed(){return new ResponseStatusException(HttpStatus.CONFLICT,"Заявка изменилась. Обновите список");}
 private static ResponseStatusException missing(){return new ResponseStatusException(HttpStatus.NOT_FOUND,"Заявка не найдена или код доступа неверен");}
}
