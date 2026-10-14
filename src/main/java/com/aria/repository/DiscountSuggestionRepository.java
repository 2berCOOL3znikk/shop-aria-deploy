package com.aria.repository;

import com.aria.entity.DiscountSuggestion;
import com.aria.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiscountSuggestionRepository extends JpaRepository<DiscountSuggestion, Long> {

    // Список предложений по статусу, отсортированные по дате предложения
    List<DiscountSuggestion> findByStatusOrderByProposedAtDesc(String status);

    // Предложения для конкретного товара
    List<DiscountSuggestion> findByProductId(Long productId);

    // Проверка существования PENDING предложения для товара
    boolean existsByProductIdAndStatus(Long productId, String status);
}
