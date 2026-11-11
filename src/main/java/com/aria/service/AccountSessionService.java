package com.aria.service;

import com.aria.repository.AppUserRepository;
import com.aria.security.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class AccountSessionService {
 private final AppUserRepository users;
 private final JwtService jwt;
 private final ApplicationIdentityService identity;
 private final AuditService audit;
 public AccountSessionService(AppUserRepository users,JwtService jwt,ApplicationIdentityService identity,AuditService audit){this.users=users;this.jwt=jwt;this.identity=identity;this.audit=audit;}
 @Transactional(readOnly=true)
 public Map<String,Object> current(String email,String token){var user=users.findByEmailIgnoreCase(email).orElseThrow();var result=new LinkedHashMap<String,Object>();result.put("email",user.getEmail());result.put("role",user.getRole().name());result.put("fullName",Objects.toString(user.getFullName(),""));result.put("expiresAt",jwt.expiresAt(token));result.put("application",identity.publicInfo());if(user.getRole()==com.aria.enums.Role.ADMIN)result.put("company",identity.company());return result;}
 @Transactional
 public void revokeAll(String email){var user=users.lockByEmail(email).orElseThrow();user.revokeTokens();users.saveAndFlush(user);audit.record("ACCOUNT_SESSIONS_REVOKED","user:"+user.getId(),"Владелец учётной записи завершил все свои сеансы; версии токенов отозваны");}
}
