package com.aria.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 100, nullable = false)
    private String email;

    @Column(length = 100)
    private String fullName;

    @Column(length = 50)
    private String phone;

    @Column(length = 500)
    private String address;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "profile_id")
    private List<CartItem> favoriteItems = new ArrayList<>(); // Избранное как список товаров

    public UserProfile() {}

    public UserProfile(String email, String name, String phone, String addr) {
        this.email = email;
        this.fullName = name;
        this.phone = phone;
        this.address = addr;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String e) { this.email = e; }
    public String getFullName() { return fullName; }
    public void setFullName(String n) { this.fullName = n; }
    public String getPhone() { return phone; }
    public void setPhone(String p) { this.phone = p; }
    public String getAddress() { return address; }
    public void setAddress(String a) { this.address = a; }
    public List<CartItem> getFavoriteItems() { return favoriteItems; }
}