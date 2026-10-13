package com.aria.controller;

import com.aria.entity.UserProfile;
import com.aria.service.ProfileService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService service;

    public ProfileController(ProfileService service) {
        this.service = service;
    }

    @GetMapping("/{email}")
    public Map<String, Object> getProfile(@PathVariable String email) {
        return service.getByEmail(email)
                .map(p -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("found", true);
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", p.getId());
                    data.put("email", p.getEmail());
                    data.put("fullName", p.getFullName());
                    data.put("phone", p.getPhone());
                    data.put("address", p.getAddress());
                    map.put("data", data);
                    return map;
                })
                .orElse(Map.of("found", false));
    }

    @PostMapping
    public UserProfile saveProfile(@RequestBody Map<String, String> data) {
        return service.createOrUpdate(
                data.getOrDefault("email", ""),
                data.getOrDefault("fullName", ""),
                data.getOrDefault("phone", ""),
                data.getOrDefault("address", "")
        );
    }
}