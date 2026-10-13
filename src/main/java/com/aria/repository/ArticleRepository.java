package com.aria.repository;

import com.aria.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    // Spring Data JPA автоматически создаст методы save(), findAll(), findById() и т.д.
}