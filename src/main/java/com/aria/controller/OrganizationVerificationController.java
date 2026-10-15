package com.aria.controller;

import com.aria.entity.AppUser;
import com.aria.entity.OrganizationVerification;
import com.aria.repository.AppUserRepository;
import com.aria.repository.OrganizationVerificationRepository;
import com.aria.service.SocialService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationVerificationController {
    private final AppUserRepository users;
    private final OrganizationVerificationRepository verifications;
    private final SocialService social;

    public OrganizationVerificationController(AppUserRepository users, OrganizationVerificationRepository verifications, SocialService social) {
        this.users = users;
        this.verifications = verifications;
        this.social = social;
    }

    @GetMapping("/verification")
    public Map<String, Object> status(Authentication authentication) {
        AppUser user = requireOrg(authentication);
        OrganizationVerification record = verifications.findByUserId(user.getId()).orElse(null);
        if (record == null) return Map.of("status", "NOT_SUBMITTED");
        Map<String, Object> result = new HashMap<>();
        result.put("id", record.getId());
        result.put("organizationName", record.getOrganizationName());
        result.put("status", record.getStatus());
        result.put("submittedAt", record.getSubmittedAt());
        result.put("reviewNote", record.getReviewNote());
        return result;
    }

    @PostMapping("/verification")
    public Map<String, Object> submit(@RequestBody Map<String, String> request, Authentication authentication) {
        AppUser user = requireOrg(authentication);
        String name = request.getOrDefault("organizationName", "").trim();
        String registrationNumber = request.getOrDefault("registrationNumber", "").trim();
        if (name.isBlank() || registrationNumber.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажите название организации и регистрационный номер");
        }
        OrganizationVerification record = verifications.findByUserId(user.getId()).orElseGet(OrganizationVerification::new);
        if ("VERIFIED".equals(record.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Организация уже проверена");
        record.setUserId(user.getId());
        record.setOrganizationName(name);
        record.setRegistrationNumber(registrationNumber);
        record.setStatus("PENDING_VERIFICATION");
        record.setSubmittedAt(LocalDateTime.now());
        record.setReviewedAt(null);
        record.setReviewerEmail(null);
        record.setReviewNote(null);
        OrganizationVerification saved = verifications.save(record);
        return Map.of("id", saved.getId(), "status", saved.getStatus(), "organizationName", saved.getOrganizationName());
    }

    @PostMapping("/offers/{offerId}/claim")
    public Map<String, Object> claim(@PathVariable Long offerId, Authentication authentication) {
        AppUser user = requireOrg(authentication);
        OrganizationVerification record = verifications.findByUserId(user.getId())
                .filter(v -> "VERIFIED".equals(v.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Организация должна пройти проверку"));
        var offer = social.claimOffer(offerId, user.getId());
        return Map.of("message", "Лот зарезервирован", "offerId", offer.getId(), "status", offer.getStatus(),
                "organizationId", record.getUserId(), "quantity", offer.getQuantityClaimed());
    }

    private AppUser requireOrg(Authentication authentication) {
        if (authentication == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        AppUser user = users.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (user.getRole() != com.aria.enums.Role.ORG) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Требуется роль ORG");
        return user;
    }
}