package com.aria.config;

import com.aria.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .headers(h -> h.frameOptions(f -> f.disable()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/hello",
                                "/actuator/health",
                                "/", "/*.html", "/css/**", "/js/**", "/images/**", "/styles.css", "/control.css", "/favicon.ico",
                                "/h2-console/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/products/social").hasAnyRole("ADMIN", "ORG")
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/social/offers").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/social/impact").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/feedback").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/social/offers").hasRole("ADMIN")
                        .requestMatchers("/api/social/offers/*/claim", "/api/social/my-claims", "/api/social/my-claims/**").hasRole("ORG")
                        .requestMatchers("/api/aria/**", "/api/aria/suggestions/**", "/api/admin/**", "/api/articles/**", "/api/debug/**", "/api/feedback/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/api/stats/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/reviews/product/**").permitAll()
                        .requestMatchers("/api/cart/**", "/api/favorites/**", "/api/orders/**",
                                "/api/profile/**", "/api/reviews/**")
                        .authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        // Не представился / билет не прочитан -> 401
                        .authenticationEntryPoint((req, res, e) -> {
                            res.setStatus(401);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"status\":\"error\",\"message\":\"Требуется авторизация (Bearer токен)\"}");
                        })
                        // Представился, но роль не та -> 403
                        .accessDeniedHandler((req, res, e) -> {
                            res.setStatus(403);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"status\":\"error\",\"message\":\"Недостаточно прав для этого действия\"}");
                        })
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
