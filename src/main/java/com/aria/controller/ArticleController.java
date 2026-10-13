package com.aria.controller;

import com.aria.entity.Article;
import com.aria.service.ArticleService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService service;

    public ArticleController(ArticleService service) {
        this.service = service;
    }

    // Получить все статьи
    @GetMapping
    public List<Article> getAll() {
        return service.getAllArticles();
    }

    // Добавить статью вручную (как было раньше)
    @PostMapping
    public Article create(@RequestBody Article article) {
        return service.saveArticle(article);
    }

    // Временно меняем на GetMapping для тестирования через браузер
    @GetMapping("/parse")
    public Article parseWebsite(@RequestParam String url) {
        return service.parseAndSave(url);
    }

    // Удалить статью по id
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }


}