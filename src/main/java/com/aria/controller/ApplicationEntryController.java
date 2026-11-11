package com.aria.controller;

import com.aria.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class ApplicationEntryController {
 private final ApplicationIdentityService identity;
 private final AccountSessionService sessions;
 public ApplicationEntryController(ApplicationIdentityService identity,AccountSessionService sessions){this.identity=identity;this.sessions=sessions;}
 @GetMapping("/") public String start(){return "redirect:/start.html";}
 @ResponseBody @GetMapping("/api/application") public Object application(){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(identity.publicInfo());}
 @ResponseBody @GetMapping("/api/auth/session") public Object session(Authentication authentication,HttpServletRequest request){String header=request.getHeader("Authorization");if(header==null||!header.startsWith("Bearer "))throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED);return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(sessions.current(authentication.getName(),header.substring(7)));}
 @ResponseBody @PostMapping("/api/auth/logout-all") public Object logoutAll(Authentication authentication){sessions.revokeAll(authentication.getName());return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();}
}
