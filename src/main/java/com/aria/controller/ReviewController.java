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

    @PostMapping
    public Review addReview(@RequestBody Map<String, Object> data) {
        Long prodId = Long.valueOf(data.get("productId").toString());
        String author = (String) data.getOrDefault("author", "Аноним");
        String comment = (String) data.getOrDefault("comment", "");
        Integer rating = Integer.valueOf(data.getOrDefault("rating", 5).toString());

        Product product = productRepo.findById(prodId)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

        Review review = new Review(product, author, comment, rating);
        return reviewRepo.save(review);
    }
}