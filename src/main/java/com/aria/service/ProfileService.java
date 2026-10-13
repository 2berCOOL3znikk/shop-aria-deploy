package com.aria.service;

import com.aria.entity.UserProfile;
import com.aria.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ProfileService {

    private final UserProfileRepository repo;

    public ProfileService(UserProfileRepository repo) {
        this.repo = repo;
    }

    public Optional<UserProfile> getByEmail(String email) {
        return repo.findByEmail(email);
    }

    @Transactional
    public UserProfile createOrUpdate(String email, String name, String phone, String address) {
        UserProfile profile = repo.findByEmail(email).orElse(new UserProfile());
        profile.setEmail(email);
        profile.setFullName(name);
        profile.setPhone(phone);
        profile.setAddress(address);
        return repo.save(profile);
    }
}