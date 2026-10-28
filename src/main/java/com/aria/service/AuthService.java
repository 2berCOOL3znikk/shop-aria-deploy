package com.aria.service;

import com.aria.entity.AppUser;
import com.aria.enums.Role;
import com.aria.repository.AppUserRepository;
import com.aria.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CaptchaService captchaService;
    private final LoginAttemptService attempts;

    public AuthService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, AuthenticationManager authenticationManager,
                       CaptchaService captchaService, LoginAttemptService attempts) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.captchaService = captchaService;
        this.attempts=attempts;
    }

    /**
     * Регистрация с защитой от ботов:
     * 1. Honeypot поле "website" должно быть пустым
     * 2. Опциональная капча должна быть верной (если предоставлена)
     */
    public Map<String, Object> register(String email, String password, String fullName,
                                         String website, String captchaId, String captchaAnswer) {
        email=normalizeEmail(email);
        validatePassword(password);
        if(fullName==null || fullName.isBlank() || fullName.length()>255) throw new IllegalArgumentException("Укажите имя (до 255 символов)");
        // Honeypot проверка: если поле заполнено — это бот
        if (website != null && !website.trim().isEmpty()) {
            throw new IllegalArgumentException("bot detected");
        }

        // Капча проверка: если captchaId предоставлен, ответ должен быть верным
        if (!captchaService.verifyAnswer(captchaId, captchaAnswer)) throw new IllegalArgumentException("Пройдите проверку регистрации заново");

        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Регистрация с этим email недоступна. Попробуйте войти");
        }

        AppUser appUser = new AppUser();
        appUser.setEmail(email);
        appUser.setPassword(passwordEncoder.encode(password));
        appUser.setFullName(fullName.trim());
        appUser.setRole(Role.USER);

        appUserRepository.save(appUser);

        String token = jwtService.generateToken(email, appUser.getRole());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("role", appUser.getRole().name());
        response.put("email", email);

        return response;
    }

    public Map<String, Object> login(String email, String password) {
        email=normalizeEmail(email);
        attempts.check(email);
        try {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );
        } catch(org.springframework.security.core.AuthenticationException exception) {
            attempts.failed(email);
            throw new org.springframework.security.authentication.BadCredentialsException("Неверный email или пароль");
        }
        attempts.succeeded(email);

        AppUser appUser = appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        email=appUser.getEmail();
        String token = jwtService.generateToken(email, appUser.getRole());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("role", appUser.getRole().name());
        response.put("email", email);

        return response;
    }

    public static String normalizeEmail(String email) {
        if(email==null || email.isBlank() || email.length()>255) throw new IllegalArgumentException("Укажите корректный email");
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    @org.springframework.transaction.annotation.Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        validatePassword(newPassword);
        AppUser user=appUserRepository.lockByEmail(email).orElseThrow(()->new IllegalArgumentException("Пользователь не найден"));
        String attemptKey=normalizeEmail(email);
        attempts.check(attemptKey);
        if(!passwordEncoder.matches(currentPassword,user.getPassword())) {
            attempts.failed(attemptKey);
            throw new IllegalArgumentException("Текущий пароль неверен");
        }
        if(passwordEncoder.matches(newPassword,user.getPassword())) throw new IllegalArgumentException("Новый пароль должен отличаться от текущего");
        attempts.succeeded(attemptKey);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.revokeTokens();
        appUserRepository.save(user);
    }
    public static void validatePassword(String password) {
        if(password==null || password.length()<12 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)
            throw new IllegalArgumentException("Пароль: не менее 12 символов и не более 72 байт UTF-8");
    }
}
