package com.aria.controller;
import com.aria.service.WorkspaceActivationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/company-activation")
public class WorkspaceActivationController {
 private final WorkspaceActivationService activation;
 public WorkspaceActivationController(WorkspaceActivationService activation){this.activation=activation;}
 public record Probe(@NotBlank @Size(max=36) String requestId,@NotBlank @Pattern(regexp="[a-f0-9]{32}") String nonce){}
 public record Claim(@NotBlank @Size(max=36) String requestId,@NotBlank @Size(max=72) String password,@AssertTrue boolean reviewed){@Override public String toString(){return "WorkspaceClaim[redacted]";}}
 private <T> ResponseEntity<T> response(T body){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);}
 @GetMapping("/{id}") public Object info(@PathVariable String id,@RequestHeader(value="X-ARIA-Activation-Code",required=false) String code,HttpServletRequest request){return response(activation.info(id,code,request.getRemoteAddr()));}
 @PostMapping("/attest") public Object attest(@RequestHeader(value="X-ARIA-Provision-Proof",required=false) String signature,@Valid @RequestBody Probe p){return response(activation.attest(signature,p.requestId,p.nonce));}
 @PostMapping("/claim") public Object claim(@RequestHeader(value="X-ARIA-Activation-Code",required=false) String code,@Valid @RequestBody Claim c,HttpServletRequest request){return response(activation.claim(c.requestId,code,c.password,c.reviewed,request.getRemoteAddr()));}
}
