package com.aria.service;

import com.aria.entity.CompanyRegistrationRequest;
import com.aria.repository.CompanyRegistrationRequestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

/** A durable application, never an assertion that a tenant/database already exists. */
@Service
public class CompanySignupService {
 private final boolean enabled;
 private final CompanyRegistrationRequestRepository requests;
 private final CaptchaService captcha;
 private final AuditService audit;
 private final Map<String,Limit> limits=new HashMap<>();
 private record Limit(int used,Instant until){}
 public CompanySignupService(@Value("${aria.company-signup.enabled:false}") boolean enabled,
       @Value("${aria.organization.key:demo}") String binding,CompanyRegistrationRequestRepository requests,CaptchaService captcha,AuditService audit){
  if(enabled&&!"aria-registration".equals(binding))throw new IllegalStateException("Company applications require the separate aria-registration database binding");
  this.enabled=enabled;this.requests=requests;this.captcha=captcha;this.audit=audit;
 }
 public boolean available(){return enabled;}
 public void requireDesk(){if(!enabled)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Регистрация компаний здесь не включена");}
 public synchronized void rate(String address,int maximum,String operation){
  requireDesk();Instant now=Instant.now();limits.entrySet().removeIf(e->!e.getValue().until.isAfter(now));String key=operation+":"+address;
  var limit=limits.get(key);if(limit==null&&limits.size()>=10000||limit!=null&&limit.used>=maximum)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Слишком много запросов. Повторите позже");
  limits.put(key,new Limit(limit==null?1:limit.used+1,limit==null?now.plusSeconds(3600):limit.until));
 }
 public Map<String,String> challenge(String address){rate(address,60,"challenge");return captcha.createChallenge();}
 @Transactional
 public Map<String,Object> submit(String id,String token,String company,String owner,String email,boolean processing,boolean contact,String website,String challengeId,String answer,String address){
  requireDesk();validateId(id);validateToken(token);
  email=AuthService.normalizeEmail(email);company=company.trim();owner=owner.trim();
  if(!processing||!contact||company.isBlank()||company.length()>200||owner.isBlank()||owner.length()>120||website!=null&&!website.isBlank())throw new IllegalArgumentException("Проверьте данные и отдельные согласия заявки");
  var existing=requests.lockById(id);
  if(existing.isPresent()){
   var r=existing.get();authorize(r,token);
   if(!r.companyName.equals(company)||!r.ownerName.equals(owner)||!r.ownerEmail.equals(email))throw new ResponseStatusException(HttpStatus.CONFLICT,"Номер заявки уже используется для других данных");
   return publicView(r);
  }
  rate(address,10,"submit");
  if(!captcha.verifyAnswer(challengeId,answer))throw new IllegalArgumentException("Пройдите проверку регистрации заново");
  if(requests.count()>=10000)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Приём заявок временно приостановлен");
  var r=new CompanyRegistrationRequest();r.id=id;r.accessHash=hash(token);r.companyName=company;r.ownerName=owner;r.ownerEmail=email;r.processingConsent=processing;r.contactConsent=contact;r.createdAt=LocalDateTime.now();r.updatedAt=r.createdAt;
  requests.saveAndFlush(r);audit.record("COMPANY_APPLICATION_SUBMITTED","company-request:"+id,"Заявка сохранена; кабинет и учётная запись ещё не созданы");return publicView(r);
 }
 @Transactional(readOnly=true)
 public Map<String,Object> status(String id,String token,String address){requireDesk();rate(address,120,"status");validateId(id);validateToken(token);var r=requests.findById(id).orElseThrow(CompanySignupService::notFound);authorize(r,token);return publicView(r);}
 @Transactional(readOnly=true)
 public List<Map<String,Object>> list(){requireDesk();return requests.findTop100ByOrderByCreatedAtDesc().stream().map(this::operatorView).toList();}
 @Transactional
 public Map<String,Object> review(String id,long version,String next,String note,boolean identityChecked){
  requireDesk();var r=requests.lockById(id).orElseThrow(CompanySignupService::notFound);
  if(r.version!=version)throw new ResponseStatusException(HttpStatus.CONFLICT,"Заявка изменилась. Обновите список");
  if(note==null||note.trim().length()<10||note.length()>700)throw new IllegalArgumentException("Укажите основание проверки от 10 до 700 символов");
  boolean allowed=r.status.equals("SUBMITTED")&&Set.of("IN_REVIEW","REJECTED").contains(next)||r.status.equals("IN_REVIEW")&&Set.of("VERIFIED","REJECTED").contains(next);
  if(!allowed)throw new ResponseStatusException(HttpStatus.CONFLICT,"Этот переход статуса недоступен");
  if(next.equals("VERIFIED")&&!identityChecked)throw new IllegalArgumentException("Подтвердите отдельную проверку контакта и полномочий владельца");
  var actor=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();r.status=next;r.reviewedBy=actor==null?"operator":actor.getName();r.reviewNote=note.trim();r.reviewedAt=LocalDateTime.now();r.updatedAt=r.reviewedAt;
  requests.flush();audit.record("COMPANY_APPLICATION_REVIEWED","company-request:"+id,next+"; окружение не предоставлено");return operatorView(r);
 }
 private Map<String,Object> publicView(CompanyRegistrationRequest r){var v=new LinkedHashMap<String,Object>();v.put("id",r.id);v.put("companyName",r.companyName);v.put("email",mask(r.ownerEmail));v.put("status",r.status);v.put("createdAt",r.createdAt);v.put("updatedAt",r.updatedAt);v.put("workspaceReady",false);v.put("accountCreated",false);v.put("emailAutomaticallyVerified",false);return v;}
 private Map<String,Object> operatorView(CompanyRegistrationRequest r){var v=publicView(r);v.put("ownerName",r.ownerName);v.put("ownerEmail",r.ownerEmail);v.put("version",r.version);v.put("processingConsent",r.processingConsent);v.put("contactConsent",r.contactConsent);v.put("consentVersion",r.consentVersion);v.put("reviewedBy",r.reviewedBy);v.put("reviewedAt",r.reviewedAt);v.put("reviewNote",r.reviewNote);return v;}
 private static String mask(String email){int at=email.indexOf('@');return at<1?"•••":email.substring(0,1)+"•••"+email.substring(at);}
 private static void validateId(String id){try{if(!UUID.fromString(id).toString().equals(id))throw new Exception();}catch(Exception e){throw notFound();}}
 private static void validateToken(String token){if(token==null||!token.matches("[a-f0-9]{64}"))throw notFound();}
 public static String hash(String token){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException("SHA-256 unavailable");}}
 private static void authorize(CompanyRegistrationRequest r,String token){if(!MessageDigest.isEqual(r.accessHash.getBytes(StandardCharsets.US_ASCII),hash(token).getBytes(StandardCharsets.US_ASCII)))throw notFound();}
 private static ResponseStatusException notFound(){return new ResponseStatusException(HttpStatus.NOT_FOUND,"Заявка не найдена или код доступа неверен");}
}
