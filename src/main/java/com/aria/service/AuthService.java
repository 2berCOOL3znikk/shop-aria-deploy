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

    public AuthService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, AuthenticationManager authenticationManager,
                       CaptchaService captchaService) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.captchaService = captchaService;
    }

    /**
     * Регистрация с защитой от ботов:
     * 1. Honeypot поле "website" должно быть пустым
     * 2. Опциональная капча должна быть верной (если предоставлена)
     */
    public Map<String, Object> register(String email, String password, String fullName,
                                         String website, String captchaId, String captchaAnswer) {
        // Honeypot проверка: если поле заполнено — это бот
        if (website != null && !website.trim().isEmpty()) {
            throw new IllegalArgumentException("bot detected");
        }

        // Капча проверка: если captchaId предоставлен, ответ должен быть верным
        if (captchaId != null && !captchaId.trim().isEmpty()) {
            if (!captchaService.verifyAnswer(captchaId, captchaAnswer)) {
                throw new IllegalArgumentException("Неверный ответ на капчу");
            }
        }

        if (appUserRepository.existsByEmail(email)) {
            throw new RuntimeException("Пользователь с таким email уже существует");
        }

        AppUser appUser = new AppUser();
        appUser.setEmail(email);
        appUser.setPassword(passwordEncoder.encode(password));
        appUser.setFullName(fullName);
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
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        AppUser appUser = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        String token = jwtService.generateToken(email, appUser.getRole());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("role", appUser.getRole().name());
        response.put("email", email);

        return response;
    }
}
