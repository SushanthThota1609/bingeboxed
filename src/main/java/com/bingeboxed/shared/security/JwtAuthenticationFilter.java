// src/main/java/com/bingeboxed/shared/security/JwtAuthenticationFilter.java
package com.bingeboxed.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        
        // Public paths that don't need authentication at all (both page and API)
        if (path.startsWith("/api/auth") || 
            path.startsWith("/api/profiles/public") ||
            path.startsWith("/api/catalog") ||
            path.startsWith("/api/reviews/content") ||
            path.startsWith("/api/reviews/user") ||
            path.endsWith("/rating") ||
            path.equals("/login") || 
            path.equals("/register") ||
            path.equals("/catalog") ||
            path.startsWith("/catalog/") ||
            // Allow HTML pages to load without token
            path.equals("/") ||
            path.equals("/reviews") ||
            path.equals("/profile") ||
            path.equals("/watchlist") ||
            path.equals("/social")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Only require token for API endpoints
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(401);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized - No valid token\"}");
            return;
        }

        final String token = authHeader.substring(7);

        try {
            if (!jwtService.isTokenValid(token)) {
                response.setStatus(401);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Unauthorized - Invalid token\"}");
                return;
            }

            String email = jwtService.extractEmail(token);
            
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                if (userDetails != null) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            logger.error("Authentication error: {}", e.getMessage());
            response.setStatus(401);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\"}");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
}