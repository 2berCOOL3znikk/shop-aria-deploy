package com.aria.controller;

import com.aria.entity.AppUser;
import com.aria.enums.Role;
import com.aria.repository.AppUserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/organizations")
public class OrganizationAdminController {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public OrganizationAdminController(AppUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> request) {
        String name = request.getOrDefault("name", "").trim();
        String email = request.getOrDefault("email", "").trim().toLowerCase(java.util.Locale.ROOT);
        String password = request.getOrDefault("password", "");
        if (name.isBlank() || email.isBlank() || password.length() < 8) {
            return ResponseEntity.badRequest().body(Map.of("error", "Укажите название, email и пароль длиной минимум 8 символов"));
        }
        if (users.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Этот email уже зарегистрирован"));
        }

        AppUser organization = new AppUser();
        organization.setEmail(email);
        organization.setFullName(name);
        organization.setPassword(passwordEncoder.encode(password));
        organization.setRole(Role.ORG);
        AppUser saved = users.save(organization);
        return ResponseEntity.ok(Map.of("id", saved.getId(), "name", saved.getFullName(), "email", saved.getEmail(), "role", saved.getRole().name()));
    }
}
