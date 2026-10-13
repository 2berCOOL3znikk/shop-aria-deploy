package com.aria.config;

import com.aria.entity.AppUser;
import com.aria.enums.Role;
import com.aria.repository.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Создаем администратора, если его нет
        if (!appUserRepository.existsByEmail("admin@aria.shop")) {
            AppUser admin = new AppUser();
            admin.setEmail("admin@aria.shop");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFullName("Администратор");
            admin.setRole(Role.ADMIN);
            appUserRepository.save(admin);
            System.out.println("Создан администратор: admin@aria.shop / admin123");
        }

        // Создаем обычного пользователя, если его нет
        if (!appUserRepository.existsByEmail("user@aria.shop")) {
            AppUser user = new AppUser();
            user.setEmail("user@aria.shop");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setFullName("Тестовый пользователь");
            user.setRole(Role.USER);
            appUserRepository.save(user);
            System.out.println("Создан пользователь: user@aria.shop / user123");
        }
    }
}
