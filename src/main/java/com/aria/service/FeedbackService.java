package com.aria.service;

import com.aria.entity.Feedback;
import com.aria.repository.FeedbackRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepo;

    public FeedbackService(FeedbackRepository feedbackRepo) {
        this.feedbackRepo = feedbackRepo;
    }

    /**
     * Создаёт новое сообщение обратной связи.
     */
    @Transactional
    public Feedback createFeedback(String message, String email, Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Оценка должна быть от 1 до 5");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Сообщение не может быть пустым");
        }
        if (message.trim().length() > 2000) {
            throw new IllegalArgumentException("Сообщение должно быть не длиннее 2000 символов");
        }
        if (email != null && email.length() > 255) {
            throw new IllegalArgumentException("Email слишком длинный");
        }

        Feedback feedback = new Feedback(message.trim(), email == null ? null : email.trim(), rating);
        return feedbackRepo.save(feedback);
    }

    /**
     * Возвращает все сообщения (для админа).
     */
    @Transactional(readOnly = true)
    public List<Feedback> listAll() {
        return feedbackRepo.findByOrderByCreatedAtDesc();
    }

    /**
     * Помечает сообщение как решённое (для админа).
     */
    @Transactional
    public Feedback resolve(Long id) {
        Feedback feedback = feedbackRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Сообщение не найдено"));
        feedback.setResolved(true);
        return feedbackRepo.save(feedback);
    }
}
