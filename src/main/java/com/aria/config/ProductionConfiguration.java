package com.aria.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

/** Refuse an unsafe production configuration before opening the database or seeding users. */
@Configuration(proxyBeanMethods=false)
@Profile("production")
public class ProductionConfiguration {
    @Bean
    static BeanFactoryPostProcessor productionSafetyCheck(Environment environment) {
        validate(environment);
        return factory -> {};
    }

    static void validate(Environment env) {
        require(env.getProperty("spring.datasource.url", "").startsWith("jdbc:postgresql://"), "PostgreSQL обязателен для рабочего режима");
        require("validate".equals(env.getProperty("spring.jpa.hibernate.ddl-auto")), "Схема должна проверяться через validate");
        require(env.getProperty("spring.flyway.enabled", Boolean.class, false), "Миграции Flyway обязательны");
        for (String flag : new String[]{"aria.seed-demo-users", "aria.seed-demo-catalog", "aria.demo-market-enabled", "aria.reset-data-on-start", "spring.h2.console.enabled"}) {
            require(!env.getProperty(flag, Boolean.class, false), "Отключите параметр " + flag + " в рабочем режиме");
        }
        String email=env.getProperty("aria.admin.email", "");
        require(email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+") && !email.endsWith("@aria.shop"), "Укажите собственный email администратора");
        String password=env.getProperty("aria.admin.password", "");
        require(password.length()>=14 && password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=72 && !password.toLowerCase(java.util.Locale.ROOT).contains("admin123"), "Пароль администратора: от 14 символов до 72 байт UTF-8, без учебного значения");
        String secret=env.getProperty("jwt.secret", "");
        require(secret.length()>=48 && !secret.contains("local-development") && !secret.equals(password), "Задайте отдельный случайный ARIA_JWT_SECRET не короче 48 символов");
    }
    private static void require(boolean condition, String message) {
        if(!condition) throw new IllegalStateException(message);
    }
}
