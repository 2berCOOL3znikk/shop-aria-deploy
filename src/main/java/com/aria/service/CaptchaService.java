package com.aria.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Простая капча без внешних сервисов (Google reCAPTCHA и т.п.).
 * Использует арифметические задачи с 5-минутным TTL.
 */
@Service
public class CaptchaService {

    private static final int TTL_MINUTES = 5;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, CaptchaChallenge> challenges = new ConcurrentHashMap<>();

    private record CaptchaChallenge(String question, String answer, LocalDateTime expiresAt) {}

    /**
     * Создаёт новую капчу и возвращает её ID и вопрос.
     */
    public Map<String, String> createChallenge() {
        int a = random.nextInt(10) + 1;
        int b = random.nextInt(10) + 1;
        int answer = a + b;
        String question = "Сколько будет " + a + " + " + b + "?";

        String id = java.util.UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(TTL_MINUTES);

        challenges.put(id, new CaptchaChallenge(question, String.valueOf(answer), expiresAt));

        // Очистка просроченных капч (lazy cleanup)
        cleanupExpired();

        return Map.of("id", id, "question", question);
    }

    /**
     * Проверяет ответ на капчу.
     */
    public boolean verifyAnswer(String captchaId, String captchaAnswer) {
        if (captchaId == null || captchaAnswer == null) {
            return false;
        }

        CaptchaChallenge challenge = challenges.get(captchaId);
        if (challenge == null) {
            return false;
        }

        if (LocalDateTime.now().isAfter(challenge.expiresAt())) {
            challenges.remove(captchaId);
            return false;
        }

        boolean correct = challenge.answer().equals(captchaAnswer.trim());
        if (correct) {
            challenges.remove(captchaId); // удаляем после успешной проверки
        }
        return correct;
    }

    /**
     * Очищает просроченные капчи.
     */
    private void cleanupExpired() {
        LocalDateTime now = LocalDateTime.now();
        challenges.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt()));
    }
}
