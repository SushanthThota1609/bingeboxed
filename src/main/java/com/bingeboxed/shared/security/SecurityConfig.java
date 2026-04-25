package com.bingeboxed.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(@Lazy JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public HTML pages - no authentication required
                        .requestMatchers("/", "/login", "/register").permitAll()
                        .requestMatchers("/catalog", "/catalog/**").permitAll()
                        .requestMatchers("/profile", "/profile/**").permitAll()
                        .requestMatchers("/reviews", "/watchlist", "/watchlist/**", "/social").permitAll()
                        .requestMatchers("/recommendations", "/recommendations/**").permitAll()
                        .requestMatchers("/users/**").permitAll()
                        // Public API endpoints
                        .requestMatchers("/api/auth/**", "/api/profiles/public/**", "/api/catalog/**").permitAll()
                        .requestMatchers("/api/watchlist/user/**").permitAll()
                        .requestMatchers("/api/social/counts/**").permitAll()
                        .requestMatchers("/api/reviews/content/**", "/api/reviews/user/**").permitAll()
                        .requestMatchers("/api/reviews/*/rating").permitAll()
                        .requestMatchers("/api/users/*/profile", "/api/users/search").permitAll()
                        // Protected API endpoints - require valid JWT
                        .requestMatchers("/api/profiles/me").authenticated()
                        .requestMatchers("/api/reviews/my", "/api/reviews/my/**", "/api/reviews/stats", "/api/reviews/contains/**").authenticated()
                        .requestMatchers("/api/reviews/*").authenticated()
                        .requestMatchers("/api/friends/**").authenticated()
                        .requestMatchers("/api/watchlist").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedEntryPoint()))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return new AuthenticationEntryPoint() {
            @Override
            public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
                String acceptHeader = request.getHeader("Accept");
                if (acceptHeader != null && acceptHeader.contains("text/html")) {
                    response.sendRedirect("/login");
                } else {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Unauthorized\"}");
                }
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
