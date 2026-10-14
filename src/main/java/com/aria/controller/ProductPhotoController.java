package com.aria.controller;

import com.aria.entity.ProductPhoto;
import com.aria.repository.ProductPhotoRepository;
import com.aria.repository.ProductRepository;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

@RestController
public class ProductPhotoController {
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png");
    private final ProductRepository products;
    private final ProductPhotoRepository photos;

    public ProductPhotoController(ProductRepository products, ProductPhotoRepository photos) {
        this.products = products;
        this.photos = photos;
    }

    @GetMapping("/api/products/{id}/photo")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> get(@PathVariable Long id) {
        return photos.findByProductId(id).map(photo -> ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .cacheControl(CacheControl.noCache())
                .body(photo.getData())).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/api/admin/products/{id}/photo")
    @Transactional
    public ResponseEntity<?> upload(@PathVariable Long id, @RequestParam("photo") MultipartFile file) throws IOException {
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(Map.of("error", "Загрузите изображение размером до 5 МБ"));
        }
        if (!ALLOWED.contains(file.getContentType())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Поддерживаются JPEG и PNG"));
        }
        byte[] data = file.getBytes();
        var decoded = ImageIO.read(new ByteArrayInputStream(data));
        if (decoded == null || decoded.getWidth() > 4096 || decoded.getHeight() > 4096) {
            return ResponseEntity.badRequest().body(Map.of("error", "Файл не является корректным изображением или его размер слишком велик"));
        }
        var product = products.findById(id).orElseThrow(() -> new IllegalArgumentException("Товар не найден"));
        ProductPhoto photo = photos.findByProductId(id).orElseGet(ProductPhoto::new);
        photo.setProduct(product);
        photo.setContentType(file.getContentType());
        photo.setFileName(file.getOriginalFilename() == null ? "product-image" : file.getOriginalFilename().replaceAll("[\\r\\n\\\\/\"]", "_"));
        photo.setData(data);
        photos.save(photo);
        product.setImageUrl("/api/products/" + id + "/photo");
        products.save(product);
        return ResponseEntity.ok(Map.of("message", "Фото товара обновлено", "imageUrl", product.getImageUrl()));
    }
}
