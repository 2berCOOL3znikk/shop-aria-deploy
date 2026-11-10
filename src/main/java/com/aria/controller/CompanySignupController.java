package com.aria.controller;

import com.aria.service.CompanySignupService;
import com.aria.service.CompanyProvisioningService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
public class CompanySignupController {
 private final CompanySignupService signup;
 private final CompanyProvisioningService provisioning;
 public CompanySignupController(CompanySignupService signup,CompanyProvisioningService provisioning){this.signup=signup;this.provisioning=provisioning;}
 public record Application(@NotBlank @Pattern(regexp="[a-f0-9-]{36}") String id,@NotBlank @Pattern(regexp="[a-f0-9]{64}") String token,
     @NotBlank @Size(max=200) String companyName,@NotBlank @Size(max=120) String ownerName,@NotBlank @Email @Size(max=255) String email,
     @AssertTrue boolean processingConsent,@AssertTrue boolean contactConsent,@Size(max=200) String website,@NotBlank @Size(max=50) String captchaId,@NotBlank @Size(max=20) String captchaAnswer){
  @Override public String toString(){return "CompanyApplication[redacted]";}
 }
 public record Review(@Min(0) long version,@NotBlank @Size(max=30) String status,@NotBlank @Size(min=10,max=700) String reason,boolean identityChecked){}
 public record Prepare(@Min(0) long version){}
 public record Verify(@Min(0) long version,@NotBlank @Size(max=500) String address){}
 private <T> ResponseEntity<T> response(T value){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("Pragma","no-cache").body(value);}
 @GetMapping("/api/company-signup/capabilities") public Object capabilities(){return response(Map.of("enabled",signup.available()));}
 @GetMapping("/api/company-signup/challenge") public Object challenge(HttpServletRequest req){return response(signup.challenge(req.getRemoteAddr()));}
 @PostMapping("/api/company-signup/requests") public Object submit(@Valid @RequestBody Application a,HttpServletRequest req){signup.requireDesk();return response(signup.submit(a.id,a.token,a.companyName,a.ownerName,a.email,a.processingConsent,a.contactConsent,a.website,a.captchaId,a.captchaAnswer,req.getRemoteAddr()));}
 @GetMapping("/api/company-signup/requests/{id}") public Object status(@PathVariable String id,@RequestHeader(value="X-ARIA-Signup-Token",required=false) String token,HttpServletRequest req){signup.requireDesk();return response(signup.status(id,token,req.getRemoteAddr()));}
 @GetMapping("/api/admin/company-signup/requests") public Object list(){return response(signup.list());}
 @PostMapping("/api/admin/company-signup/requests/{id}/review") public Object review(@PathVariable String id,@Valid @RequestBody Review r){return response(signup.review(id,r.version,r.status,r.reason,r.identityChecked));}
 @PostMapping("/api/admin/company-signup/requests/{id}/prepare") public Object prepare(@PathVariable String id,@Valid @RequestBody Prepare r){return response(provisioning.prepare(id,r.version));}
 @PostMapping("/api/admin/company-signup/requests/{id}/verify") public Object verify(@PathVariable String id,@Valid @RequestBody Verify r){return response(provisioning.verify(id,r.version,r.address));}
 @PostMapping("/api/company-signup/requests/{id}/refresh-workspace") public Object refreshWorkspace(@PathVariable String id,@RequestHeader(value="X-ARIA-Signup-Token",required=false) String token,HttpServletRequest req){return response(provisioning.refresh(id,token,req.getRemoteAddr()));}
}
