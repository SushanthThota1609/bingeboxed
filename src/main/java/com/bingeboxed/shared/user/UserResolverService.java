package com.bingeboxed.shared.user;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserResolverService {

    private final JdbcTemplate jdbcTemplate;

    public UserResolverService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Long> resolveUserId(String email) {
        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT id FROM auth_users WHERE email = ?", Long.class, email);
        return ids.isEmpty() ? Optional.empty() : Optional.of(ids.get(0));
    }

    public Optional<Long> resolveUserById(Long userId) {
        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT id FROM auth_users WHERE id = ?", Long.class, userId);
        return ids.isEmpty() ? Optional.empty() : Optional.of(ids.get(0));
    }
}
