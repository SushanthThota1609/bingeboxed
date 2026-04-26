// src/main/java/com/bingeboxed/watchlist/controller/UserResolverImpl.java
package com.bingeboxed.watchlist.controller;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
public class UserResolverImpl implements UserResolver {

    private final JdbcTemplate jdbcTemplate;

    public UserResolverImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Long resolveUserIdByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        List<Long> results = jdbcTemplate.queryForList(
                "SELECT id FROM auth_users WHERE email = ?", Long.class, email);
        if (results.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return results.get(0);
    }
}