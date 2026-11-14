package com.aria.controller;
import com.aria.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/employee-invitations")
public class StaffInvitationController {
 private final StaffService service;private final LoginAttemptService limiter;
 public StaffInvitationController(StaffService service,LoginAttemptService limiter){this.service=service;this.limiter=limiter;}
 public record Token(@NotBlank @Pattern(regexp="[a-f0-9]{64}") String token){}
 public record Accept(@NotBlank @Pattern(regexp="[a-f0-9]{64}") String token,@NotBlank @Size(max=100) String name,@NotBlank @Size(min=12,max=72) String password,boolean permissionsConfirmed){}
 @PostMapping("/inspect") public ResponseEntity<?> inspect(@Valid @RequestBody Token input,HttpServletRequest request){String scope="employee-invite:"+request.getRemoteAddr();limiter.check(scope);try{return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.inspect(input.token()));}catch(RuntimeException bad){limiter.failed(scope);throw bad;}}
 @PostMapping("/accept") public ResponseEntity<?> accept(@Valid @RequestBody Accept input,HttpServletRequest request){String scope="employee-invite:"+request.getRemoteAddr();limiter.check(scope);try{var result=service.accept(input.token(),input.name(),input.password(),input.permissionsConfirmed());limiter.succeeded(scope);return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(result);}catch(RuntimeException bad){limiter.failed(scope);throw bad;}}
}
