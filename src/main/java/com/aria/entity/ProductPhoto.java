package com.aria.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "product_photos")
public class ProductPhoto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(name = "content_type", length = 80, nullable = false)
    private String contentType;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Lob
    @Column(nullable = false)
    private byte[] data;

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }
}
