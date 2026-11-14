package com.aria.controller;
import com.aria.service.CloudRegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
@RestController
public class CloudRegistrationController {
 private final CloudRegistrationService service;
 public CloudRegistrationController(CloudRegistrationService service){this.service=service;}
 public record Registration(@NotBlank @Pattern(regexp="[a-f0-9-]{36}") String id,@NotBlank @Pattern(regexp="[a-f0-9]{64}") String token,@NotBlank @Pattern(regexp="[a-f0-9]{64}") String admissionCode,@NotBlank @Email @Size(max=255) String email,@NotBlank @Size(min=12,max=72) String password,@NotBlank @Size(max=200) String companyName,@AssertTrue boolean processingConsent,@AssertTrue boolean authorityConfirmed,@NotBlank @Size(max=50) String captchaId,@NotBlank @Size(max=20) String captchaAnswer){@Override public String toString(){return "Registration[redacted]";}}
 public record Recovery(@NotBlank @Email @Size(max=255) String email,@NotBlank @Size(min=12,max=72) String password,@NotBlank @Size(max=50) String captchaId,@NotBlank @Size(max=20) String captchaAnswer){@Override public String toString(){return "Recovery[redacted]";}}
 public record Slot(@NotBlank @Pattern(regexp="[a-f0-9-]{36}") String id,@NotBlank @Pattern(regexp="business-[a-f0-9]{32}") String binding,@NotBlank @Size(max=300) String origin,@NotBlank @Pattern(regexp="[a-f0-9]{64}") String secret,@NotBlank @Pattern(regexp="[a-f0-9]{64}") String admissionCode){@Override public String toString(){return "SlotSetup[redacted]";}}
 private Object response(Object body){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);}
 @GetMapping("/api/saas/capabilities") public Object capabilities(){return response(service.capabilities());}
 @GetMapping("/api/saas/challenge") public Object challenge(HttpServletRequest r){return response(service.challenge(r.getRemoteAddr()));}
 @PostMapping("/api/saas/register") public Object register(@Valid @RequestBody Registration a,HttpServletRequest r,org.springframework.security.core.Authentication authentication){return response(service.register(new CloudRegistrationService.Signup(a.id,a.token,a.admissionCode,a.email,a.password,a.companyName,a.processingConsent,a.authorityConfirmed,a.captchaId,a.captchaAnswer),r.getRemoteAddr(),authentication==null?null:authentication.getName()));}
 @GetMapping("/api/saas/registrations/{id}") public Object status(@PathVariable String id,@RequestHeader(value="X-ARIA-Registration-Code",required=false) String code,HttpServletRequest r){return response(service.status(id,code,r.getRemoteAddr()));}
 @PostMapping("/api/saas/registrations/{id}/retry") public Object retry(@PathVariable String id,@RequestHeader(value="X-ARIA-Registration-Code",required=false) String code,HttpServletRequest r){return response(service.retry(id,code,r.getRemoteAddr()));}
 @PostMapping("/api/saas/recover") public Object recover(@Valid @RequestBody Recovery a,HttpServletRequest r){return response(service.recover(a.email,a.password,a.captchaId,a.captchaAnswer,r.getRemoteAddr()));}
 @GetMapping("/api/admin/workspace-pool") public Object pool(){return response(service.pool());}
 @PostMapping("/api/admin/workspace-pool/slots") public Object slot(@Valid @RequestBody Slot s){return response(service.addSlot(s.id,s.binding,s.origin,s.secret,s.admissionCode));}
}
