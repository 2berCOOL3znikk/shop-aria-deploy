package com.aria.controller;

import com.aria.entity.Product;
import com.aria.entity.Review;
import com.aria.repository.ProductRepository;
import com.aria.repository.ReviewRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewRepository reviewRepo;
    private final ProductRepository productRepo;

    public ReviewController(ReviewRepository reviewRepo, ProductRepository productRepo) {
        this.reviewRepo = reviewRepo;
        this.productRepo = productRepo;
    }

    @GetMapping("/product/{id}")
    public List<Map<String, Object>> getByProduct(@PathVariable Long id) {
        return reviewRepo.findByProductId(id).stream().map(review -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", review.getId());
            map.put("authorName", review.getAuthorName());
            map.put("comment", review.getComment());
            map.put("rating", review.getRating());
            map.put("createdAt", review.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
    }

    public record ReviewInput(@NotNull @Positive Long productId, @Size(max=100) String author,
                              @NotBlank @Size(max=2000) String comment, @Min(1) @Max(5) Integer rating) {}

    @PostMapping
    public Map<String, Object> addReview(@Valid @RequestBody ReviewInput data) {
        Long prodId = data.productId();
        String author = data.author();
        String comment = data.comment().trim();
        Integer rating = data.rating() == null ? 5 : data.rating();

        if (author == null || author.isBlank()) author = "Аноним";
        if (author.length() > 100 || comment.isBlank() || comment.length() > 2000 || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Проверьте имя, отзыв и оценку (1–5 звёзд, до 2000 символов)");
        }

        Product product = productRepo.findById(prodId)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));
        if (product.getStatus() != com.aria.enums.ProductStatus.ACTIVE && product.getStatus() != com.aria.enums.ProductStatus.DISCOUNTED) {
            throw new IllegalArgumentException("Отзыв можно оставить только на товар из каталога");
        }

        Review review = new Review(product, author, comment, rating);
        Review saved = reviewRepo.save(review);
        Map<String, Object> result = new HashMap<>();
        result.put("id", saved.getId());
        result.put("authorName", saved.getAuthorName());
        result.put("comment", saved.getComment());
        result.put("rating", saved.getRating());
        result.put("createdAt", saved.getCreatedAt());
        return result;
    }
}
