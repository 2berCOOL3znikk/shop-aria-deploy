package com.aria.service;
import com.aria.entity.*;
import com.aria.enums.Role;
import com.aria.repository.AppUserRepository;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** The existing isolated company boundary applies. No email delivery, role promotion or external tools. */
@Service @Transactional
public class StaffService {
 @PersistenceContext private EntityManager em;
 private final AppUserRepository users;private final PasswordEncoder passwords;private final AuditService audit;
 public StaffService(AppUserRepository users,PasswordEncoder passwords,AuditService audit){this.users=users;this.passwords=passwords;this.audit=audit;}
 private void administrator(){var a=SecurityContextHolder.getContext().getAuthentication();if(a==null||a.getAuthorities().stream().noneMatch(r->r.getAuthority().equals("ROLE_ADMIN")))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Требуется администратор компании");}
 private String actor(){return SecurityContextHolder.getContext().getAuthentication().getName();}
 private Map<String,Object> invitation(StaffInvitation i){var row=new LinkedHashMap<String,Object>();row.put("id",i.id);row.put("version",i.version);row.put("email",i.email);row.put("status",i.status.equals("PENDING")&&!i.expiresAt.isAfter(LocalDateTime.now())?"EXPIRED":i.status);row.put("expiresAt",i.expiresAt);row.put("createdAt",i.createdAt);row.put("role","WAREHOUSE");return row;}
 private Map<String,Object> member(StaffMember m){return Map.of("id",m.id,"version",m.version,"email",m.user.getEmail(),"name",m.user.getFullName(),"active",m.active,"role",m.user.getRole().name(),"joinedAt",m.joinedAt);}
 @Transactional(readOnly=true) public Object list(){administrator();return Map.of("members",em.createQuery("select m from StaffMember m join fetch m.user order by m.id desc",StaffMember.class).setMaxResults(100).getResultList().stream().map(this::member).toList(),"invitations",em.createQuery("select i from StaffInvitation i order by i.id desc",StaffInvitation.class).setMaxResults(100).getResultList().stream().map(this::invitation).toList());}
 public Object invite(String rawEmail,boolean confirmed){
  administrator();if(!confirmed)throw new IllegalArgumentException("Подтвердите права сотрудника склада");String email=rawEmail.trim().toLowerCase(Locale.ROOT);
  em.find(Organization.class,1,LockModeType.PESSIMISTIC_WRITE);
  if(users.existsByEmailIgnoreCase(email))throw new ResponseStatusException(HttpStatus.CONFLICT,"Этот аккаунт уже существует. Приглашение не меняет его права");
  var pending=em.createQuery("select i from StaffInvitation i where i.email=:email and i.status='PENDING'",StaffInvitation.class).setParameter("email",email).getResultList();
  for(var i:pending){if(i.expiresAt.isAfter(LocalDateTime.now()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Для этого email уже есть действующее приглашение");i.status="EXPIRED";}
  long active=em.createQuery("select count(m) from StaffMember m",Long.class).getSingleResult(),count=em.createQuery("select count(i) from StaffInvitation i where i.status='PENDING' and i.expiresAt>:now",Long.class).setParameter("now",LocalDateTime.now()).getSingleResult();
  if(active+count>=100)throw new ResponseStatusException(HttpStatus.CONFLICT,"Достигнут лимит сотрудников и приглашений");
  byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);String token=HexFormat.of().formatHex(bytes);var i=new StaffInvitation();i.email=email;i.tokenHash=hash(token);i.createdAt=LocalDateTime.now();i.expiresAt=i.createdAt.plusHours(24);i.createdBy=actor();em.persist(i);em.flush();audit.record("STAFF_INVITED","staff-invitation:"+i.id,"Выдано приглашение на склад; доступов к ценам, AI и внешним интеграциям нет");
  return Map.of("invitation",invitation(i),"token",token);
 }
 private StaffInvitation pending(String token,boolean locked){
  if(token==null||!token.matches("[a-f0-9]{64}"))throw invalid();var q=em.createQuery("select i from StaffInvitation i where i.tokenHash=:hash",StaffInvitation.class).setParameter("hash",hash(token));if(locked)q.setLockMode(LockModeType.PESSIMISTIC_WRITE);var rows=q.getResultList();if(rows.isEmpty())throw invalid();var i=rows.getFirst();if(!i.status.equals("PENDING")||!i.expiresAt.isAfter(LocalDateTime.now()))throw invalid();return i;
 }
 private ResponseStatusException invalid(){return new ResponseStatusException(HttpStatus.BAD_REQUEST,"Приглашение недействительно или срок действия истёк");}
 @Transactional(readOnly=true) public Object inspect(String token){var i=pending(token,false);return Map.of("email",i.email,"role","WAREHOUSE","company",em.find(Organization.class,1).name,"expiresAt",i.expiresAt);}
 public Object accept(String token,String name,String password,boolean confirmed){
  if(!confirmed)throw new IllegalArgumentException("Подтвердите принятие доступа к складу");if(password.getBytes(StandardCharsets.UTF_8).length>72)throw new IllegalArgumentException("Пароль слишком длинный");var i=pending(token,true);
  if(users.existsByEmailIgnoreCase(i.email))throw new ResponseStatusException(HttpStatus.CONFLICT,"Этот аккаунт уже существует. Приглашение не меняет его права");
  var user=new AppUser(i.email,passwords.encode(password),name.trim(),Role.WAREHOUSE);users.saveAndFlush(user);var m=new StaffMember();m.user=user;m.joinedAt=LocalDateTime.now();em.persist(m);i.status="ACCEPTED";i.acceptedUserId=user.getId();em.flush();audit.record("STAFF_JOINED","staff-member:"+m.id,"Сотрудник принял ограниченный доступ к складу");return Map.of("created",true,"email",i.email,"role","WAREHOUSE");
 }
 public Object revoke(Long id,long version,String reason){administrator();var i=em.find(StaffInvitation.class,id,LockModeType.PESSIMISTIC_WRITE);if(i==null)throw invalid();if(i.version!=version)throw new ResponseStatusException(HttpStatus.CONFLICT,"Приглашение изменилось. Обновите список");if(!i.status.equals("PENDING"))throw new ResponseStatusException(HttpStatus.CONFLICT,"Приглашение уже закрыто");i.status="REVOKED";audit.record("STAFF_INVITATION_REVOKED","staff-invitation:"+id,reason);return Map.of("revoked",true);}
 public Object access(Long id,long version,boolean active,String reason){administrator();var m=em.find(StaffMember.class,id,LockModeType.PESSIMISTIC_WRITE);if(m==null||m.user.getRole()!=Role.WAREHOUSE)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Сотрудник не найден");if(m.version!=version)throw new ResponseStatusException(HttpStatus.CONFLICT,"Доступ изменился. Обновите список");if(m.active!=active){m.active=active;m.changedAt=LocalDateTime.now();m.user.revokeTokens();audit.record(active?"STAFF_ENABLED":"STAFF_DISABLED","staff-member:"+id,reason);}em.flush();return member(m);}
 private String hash(String token){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
