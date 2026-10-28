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
    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.core.env.Environment environment;

    public DataSeeder(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        adminEmail=com.aria.service.AuthService.normalizeEmail(adminEmail);
        if(environment.acceptsProfiles(org.springframework.core.env.Profiles.of("production"))) {
            for(var entry:java.util.Map.of("admin@aria.shop","admin123","user@aria.shop","user123","fund@aria.shop","fund123").entrySet()) {
                appUserRepository.findByEmail(entry.getKey()).ifPresent(user->{
                    if(passwordEncoder.matches(entry.getValue(),user.getPassword()))
                        throw new IllegalStateException("В базе остался учебный пароль. Смените его до включения рабочего режима");
                });
            }
        }
        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException("Задайте непустой пароль администратора в ARIA_ADMIN_PASSWORD");
        }
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalStateException("Задайте ARIA_JWT_SECRET длиной не менее 32 символов");
        }
        var existingAdmin=appUserRepository.findByEmailIgnoreCase(adminEmail);
        if(existingAdmin.isPresent() && existingAdmin.get().getRole()!=Role.ADMIN)
            throw new IllegalStateException("Email владельца уже принадлежит пользователю без роли администратора. Выберите отдельный аккаунт владельца");
        if (existingAdmin.isEmpty()) {
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
