package com.aria.controller;

import com.aria.entity.UserProfile;
import com.aria.service.ProfileService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.HashMap;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService service;

    public ProfileController(ProfileService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public Map<String, Object> getProfile(Authentication authentication) {
        String email = authentication.getName();
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

    public record ProfileInput(@Size(max=100) String fullName, @Size(max=50) String phone, @Size(max=500) String address) {}

    @PostMapping
    public UserProfile saveProfile(@Valid @RequestBody ProfileInput data, Authentication authentication) {
        return service.createOrUpdate(
                authentication.getName(),
                data.fullName() == null ? "" : data.fullName().trim(),
                data.phone() == null ? "" : data.phone().trim(),
                data.address() == null ? "" : data.address().trim()
        );
    }
}
