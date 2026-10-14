package com.aria.controller;

import com.aria.entity.Feedback;
import com.aria.service.FeedbackService;
import com.aria.service.CaptchaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final CaptchaService captchaService;

    public FeedbackController(FeedbackService feedbackService, CaptchaService captchaService) {
        this.feedbackService = feedbackService;
        this.captchaService = captchaService;
    }

    /**
     * Создать сообщение обратной связи (permitAll с honeypot+captcha).
     */
    @PostMapping
    public ResponseEntity<?> createFeedback(@RequestBody Map<String, String> request) {
        try {
            String message = request.get("message");
            String email = request.get("email");
            String ratingStr = request.get("rating");
            String website = request.get("website"); // honeypot
            String captchaId = request.get("captchaId");
            String captchaAnswer = request.get("captchaAnswer");

            // Honeypot проверка
            if (website != null && !website.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "bot detected"));
            }

            if (!captchaService.verifyAnswer(captchaId, captchaAnswer)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Решите проверочный пример"));
            }

            Integer rating = ratingStr != null ? Integer.parseInt(ratingStr) : null;
            Feedback feedback = feedbackService.createFeedback(message, email, rating);

            Map<String, Object> res = new HashMap<>();
            res.put("message", "Спасибо за обратную связь!");
            res.put("id", feedback.getId());
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ошибка при создании сообщения"));
        }
    }

    /**
     * Получить все сообщения (только для ADMIN).
     */
    @GetMapping
    public List<Feedback> getAll() {
        return feedbackService.listAll();
    }

    /**
     * Пометить сообщение как решённое (только для ADMIN).
     */
    @PatchMapping("/{id}/resolve")
    public ResponseEntity<?> resolve(@PathVariable Long id) {
        try {
            Feedback feedback = feedbackService.resolve(id);
            return ResponseEntity.ok(feedback);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
