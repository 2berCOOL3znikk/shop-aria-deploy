package com.aria.config;

import com.aria.entity.AppUser;
import com.aria.enums.Role;
import com.aria.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${aria.admin.email:admin@aria.shop}")
    private String adminEmail;
    @Value("${aria.admin.password:admin123}")
    private String adminPassword;
    @Value("${aria.seed-demo-users:true}")
    private boolean seedDemoUsers;
    @Value("${jwt.secret:}")
    private String jwtSecret;

    public DataSeeder(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException("Задайте непустой пароль администратора в ARIA_ADMIN_PASSWORD");
        }
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalStateException("Задайте ARIA_JWT_SECRET длиной не менее 32 символов");
        }
        if (!appUserRepository.existsByEmail(adminEmail)) {
            AppUser admin = new AppUser();
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setFullName("Администратор ARIA");
            admin.setRole(Role.ADMIN);
            appUserRepository.save(admin);
            System.out.println("Создан администратор: " + adminEmail);
        }

        if (!seedDemoUsers) return;

        seedUser("user@aria.shop", "user123", "Демо-покупатель", Role.USER);
        seedUser("fund@aria.shop", "fund123", "Фонд «Тёплый дом»", Role.ORG);
    }

    private void seedUser(String email, String password, String name, Role role) {
        if (appUserRepository.existsByEmail(email)) return;
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(name);
        user.setRole(role);
        appUserRepository.save(user);
    }
}
