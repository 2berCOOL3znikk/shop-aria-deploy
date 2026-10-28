package com.aria.controller;
import com.aria.dto.AuthRequests.*;
import com.aria.service.AuthService;
import com.aria.service.CaptchaService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final CaptchaService captcha;
    public AuthController(AuthService auth,CaptchaService captcha){this.auth=auth;this.captcha=captcha;}
    @PostMapping("/register")
    public ResponseEntity<Map<String,Object>> register(@Valid @RequestBody Register input){
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(auth.register(input.email(),input.password(),input.fullName(),input.website(),input.captchaId(),input.captchaAnswer()));
    }
    @GetMapping("/captcha")
    public ResponseEntity<Map<String,String>> captcha(){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(captcha.createChallenge());}
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody Login input){
        try{return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(auth.login(input.email(),input.password()));}
        catch(BadCredentialsException exception){return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error","Неверный email или пароль"));}
    }
}
