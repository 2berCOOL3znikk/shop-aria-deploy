package com.aria.controller;

import com.aria.entity.AppUser;
import com.aria.entity.OrganizationVerification;
import com.aria.repository.AppUserRepository;
import com.aria.repository.OrganizationVerificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/organizations")
public class OrganizationVerificationAdminController {
    private final OrganizationVerificationRepository verifications;
    private final AppUserRepository users;

    public OrganizationVerificationAdminController(OrganizationVerificationRepository verifications, AppUserRepository users) {
        this.verifications = verifications;
        this.users = users;
    }

    @GetMapping("/verification-requests")
    public List<Map<String, Object>> pending() {
        return verifications.findAllByStatusOrderBySubmittedAtAsc("PENDING_VERIFICATION").stream().map(record -> {
            AppUser user = users.findById(record.getUserId()).orElse(null);
            Map<String, Object> row = new HashMap<>();
            row.put("id", record.getId());
            row.put("organizationId", record.getUserId());
            row.put("organizationName", record.getOrganizationName());
            row.put("registrationNumber", record.getRegistrationNumber());
            row.put("email", user == null ? "" : user.getEmail());
            row.put("status", record.getStatus());
            row.put("submittedAt", record.getSubmittedAt());
            return row;
        }).toList();
    }

    @PostMapping("/verification-requests/{id}/approve")
    public Map<String, Object> approve(@PathVariable Long id, Authentication authentication) {
        OrganizationVerification record = verifications.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Заявка не найдена"));
        if (!"PENDING_VERIFICATION".equals(record.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Заявка уже рассмотрена");
        }
        record.setStatus("VERIFIED");
        record.setReviewedAt(LocalDateTime.now());
        record.setReviewerEmail(authentication == null ? "" : authentication.getName());
        verifications.save(record);
        return Map.of("id", record.getId(), "status", record.getStatus());
    }
}