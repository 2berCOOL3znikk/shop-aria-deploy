package com.aria.controller;
import com.aria.dto.AuthRequests.PasswordChange;
import com.aria.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("/api/profile/security")
public class AccountSecurityController {
    private final AuthService auth;
    public AccountSecurityController(AuthService auth){this.auth=auth;}
    @PostMapping("/password")
    public Map<String,String> changePassword(@Valid @RequestBody PasswordChange input, Authentication authentication) {
        auth.changePassword(authentication.getName(),input.currentPassword(),input.newPassword());
        return Map.of("message","Пароль изменён. Войдите заново на всех устройствах");
    }
}
