package com.aria.security;

import com.aria.entity.AppUser;
import com.aria.repository.AppUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AppUserRepository appUserRepository;
    private final com.aria.repository.StaffMemberRepository staff;

    public UserDetailsServiceImpl(AppUserRepository appUserRepository,com.aria.repository.StaffMemberRepository staff) {
        this.appUserRepository = appUserRepository;
        this.staff=staff;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AppUser appUser = appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + email));

        return User.builder()
                .username(appUser.getEmail())
                .password(appUser.getPassword())
                .disabled(appUser.getRole()==com.aria.enums.Role.WAREHOUSE&&!staff.findByUserId(appUser.getId()).map(m->m.active).orElse(false))
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + appUser.getRole().name())))
                .build();
    }
}
