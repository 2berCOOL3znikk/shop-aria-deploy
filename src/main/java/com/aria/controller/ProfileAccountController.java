package com.aria.controller;

import com.aria.entity.AppUser;
import com.aria.entity.UserProfile;
import com.aria.repository.AppUserRepository;
import com.aria.repository.UserProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileAccountController {
    private final AppUserRepository users;
    private final UserProfileRepository profiles;

    public ProfileAccountController(AppUserRepository users, UserProfileRepository profiles) {
        this.users = users;
        this.profiles = profiles;
    }

    @GetMapping("/{email}")
    public Map<String, Object> getAccount(@PathVariable String email, Authentication authentication) {
        if (authentication == null || !authentication.getName().equalsIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Можно просматривать только собственный профиль");
        }
        AppUser user = users.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден"));
        UserProfile profile = profiles.findByEmail(authentication.getName()).orElse(null);
        Map<String, Object> data = new HashMap<>();
        data.put("email", user.getEmail());
        data.put("fullName", profile != null && profile.getFullName() != null && !profile.getFullName().isBlank()
                ? profile.getFullName() : user.getFullName());
        data.put("phone", profile == null ? "" : profile.getPhone());
        data.put("address", profile == null ? "" : profile.getAddress());
        return Map.of("found", true, "data", data);
    }
}