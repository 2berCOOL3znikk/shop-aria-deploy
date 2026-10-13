package com.aria.service;

import com.aria.entity.Article;
import com.aria.repository.ArticleRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ArticleService {

    private final ArticleRepository repository;

    public ArticleService(ArticleRepository repository) {
        this.repository = repository;
    }

    public List<Article> getAllArticles() {
        return repository.findAll();
    }

    public Article saveArticle(Article article) {
        return repository.save(article);
    }
    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Article parseAndSave(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .get();

            Article article = new Article();
            article.setUrl(truncate(url, 1000));
            article.setParsedAt(LocalDateTime.now());

            String title = doc.title();
            article.setTitle(truncate(title != null ? title : "Без заголовка", 500));

            Element metaDesc = doc.selectFirst("meta[name=description]");
            article.setDescription(truncate(metaDesc != null ? metaDesc.attr("content") : "", 1000));

            Element ogImage = doc.selectFirst("meta[property=og:image]");
            String img = "";
            if (ogImage != null) {
                img = ogImage.attr("content");
            } else {
                Elements images = doc.select("img[src]");
                if (!images.isEmpty()) img = images.first().attr("abs:src");
            }
            article.setImageUrl(truncate(img, 2000));

            Element htmlTag = doc.selectFirst("html");
            String lang = htmlTag != null ? htmlTag.attr("lang") : "";
            article.setLanguage(truncate(lang.isEmpty() ? "unknown" : lang, 20));

            String content = extractMainContent(doc);
            article.setContent(content);
            article.setWordCount(content == null || content.isBlank() ? 0 : content.split("\\s+").length);

            return repository.save(article);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Ошибка парсинга: " + e.getMessage());
        }
    }

    private String extractMainContent(Document doc) {
        Element article = doc.selectFirst("article");
        if (article != null) return cleanText(article.text());

        Element main = doc.selectFirst("main");
        if (main != null) return cleanText(main.text());

        Elements divs = doc.select("div");
        Element biggestDiv = null;
        int maxTextLength = 0;
        for (Element div : divs) {
            String text = div.text();
            if (text.length() > maxTextLength) {
                maxTextLength = text.length();
                biggestDiv = div;
            }
        }
        if (biggestDiv != null && maxTextLength > 200) return cleanText(biggestDiv.text());

        return cleanText(doc.body().text());
    }

    private String cleanText(String text) {
        text = text.replaceAll("\\s+", " ").trim();
        if (text.length() > 5000) text = text.substring(0, 5000) + "...";
        return text;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "";
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}