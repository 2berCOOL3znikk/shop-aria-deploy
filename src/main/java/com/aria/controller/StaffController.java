package com.aria.controller;
import com.aria.service.StaffService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/control/team")
public class StaffController {
 private final StaffService service;public StaffController(StaffService service){this.service=service;}
 public record Invite(@NotBlank @Email @Size(max=255) String email,boolean permissionsConfirmed){}
 public record Change(@Min(0) long version,@NotBlank @Size(min=5,max=700) String reason){}
 @GetMapping public ResponseEntity<?> list(){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.list());}
 @PostMapping("/invitations") public ResponseEntity<?> invite(@Valid @RequestBody Invite input){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.invite(input.email(),input.permissionsConfirmed()));}
 @PostMapping("/invitations/{id}/revoke") public Object revoke(@PathVariable Long id,@Valid @RequestBody Change input){return service.revoke(id,input.version(),input.reason());}
 @PostMapping("/members/{id}/disable") public Object disable(@PathVariable Long id,@Valid @RequestBody Change input){return service.access(id,input.version(),false,input.reason());}
 @PostMapping("/members/{id}/enable") public Object enable(@PathVariable Long id,@Valid @RequestBody Change input){return service.access(id,input.version(),true,input.reason());}
}
