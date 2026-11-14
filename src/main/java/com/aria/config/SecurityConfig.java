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

    @org.springframework.beans.factory.annotation.Value("${aria.organization.key:demo}")
    private String organizationKey;

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
                .headers(h -> h.frameOptions(f -> f.sameOrigin())
                        .contentSecurityPolicy(c -> c.policyDirectives("frame-ancestors 'self'; object-src 'none'; base-uri 'self'")))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST,"/api/webhooks/woocommerce/*/*").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/auth/capabilities").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/application").permitAll()
                        .requestMatchers("/api/auth/session","/api/auth/logout-all").authenticated()
                        .requestMatchers("/api/company-signup/**").permitAll()
                        .requestMatchers("/api/company-activation/**").permitAll()
                        .requestMatchers(HttpMethod.POST,"/api/employee-invitations/inspect","/api/employee-invitations/accept").permitAll()
                        .requestMatchers((org.springframework.security.web.util.matcher.RequestMatcher) this::warehouseRoute).hasAnyRole("ADMIN","WAREHOUSE")
                        .requestMatchers((org.springframework.security.web.util.matcher.RequestMatcher) this::warehouseRemainder).denyAll()
                        // A partner explicitly created inside this company can use only its
                        // social workflow. Keep all catalogue/admin/customer APIs closed.
                        .requestMatchers((org.springframework.security.web.util.matcher.RequestMatcher) request ->
                                !"demo".equals(organizationKey) && partnerRoute(request))
                        .hasAnyRole("ADMIN","ORG")
                        // Only the preserved demo exposes customer APIs/registration publicly.
                        // Silo companies must not expose their imported business catalogue anonymously.
                        .requestMatchers((org.springframework.security.web.util.matcher.RequestMatcher) request ->
                                !"demo".equals(organizationKey) && request.getServletPath().startsWith("/api/")
                                        && !request.getServletPath().equals("/api/auth/login"))
                        .hasRole("ADMIN")
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/hello",
                                "/actuator/health",
                                "/", "/*.html", "/css/**", "/js/**", "/images/**", "/styles.css", "/control.css", "/favicon.ico",
                                "/h2-console/**", "/sw.js", "/manifest.webmanifest", "/manifest-en.webmanifest"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/products/social").hasAnyRole("ADMIN", "ORG")
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/social/offers").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/social/impact").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/feedback").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/assistant/query").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/assistant/status").permitAll()
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

    private boolean partnerRoute(jakarta.servlet.http.HttpServletRequest request) {
        String path=request.getServletPath(),method=request.getMethod();
        if("GET".equals(method))return java.util.Set.of("/api/organizations/verification","/api/social/offers",
                "/api/social/impact","/api/social/my-claims","/api/products/store-info").contains(path)
                ||path.matches("/api/social/my-claims/[1-9][0-9]*/act")||path.matches("/api/social/offers/[1-9][0-9]*/photo");
        if("POST".equals(method))return path.equals("/api/organizations/verification")
                ||path.matches("/api/social/offers/[1-9][0-9]*/claim")
                ||path.matches("/api/organizations/offers/[1-9][0-9]*/claim");
        return false;
    }

    private boolean warehouseRoute(jakarta.servlet.http.HttpServletRequest request) {
        if(!warehouseRemainder(request))return false;
        String path=request.getServletPath(),method=request.getMethod();
        if("GET".equals(method))return java.util.Set.of("/api/admin/control/products","/api/admin/control/lots","/api/admin/control/suppliers","/api/admin/control/policies","/api/admin/control/movements","/api/admin/control/workspace/flow","/api/profile/me").contains(path)
                ||path.matches("/api/admin/control/(lots|products)/[1-9][0-9]*")
                ||path.matches("/api/admin/control/workspace/flow/[1-9][0-9]*")
                ||path.matches("/api/products/[1-9][0-9]*/photo")
                ||path.matches("/api/profile/[^/]{1,255}");
        if("POST".equals(method))return path.equals("/api/admin/control/lots")||path.equals("/api/profile")||path.equals("/api/profile/security/password");
        if("PUT".equals(method))return path.matches("/api/admin/control/lots/[1-9][0-9]*(/handling)?");
        return "DELETE".equals(method)&&path.matches("/api/admin/control/lots/[1-9][0-9]*");
    }
    private boolean warehouseRemainder(jakarta.servlet.http.HttpServletRequest request) {
        var current=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return request.getServletPath().startsWith("/api/")&&current!=null&&current.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_WAREHOUSE"));
    }
}
