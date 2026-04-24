package com.bingeboxed.social.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "friendships",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id_1", "user_id_2"}))
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id_1", nullable = false)
    private Long userId1;

    @Column(name = "user_id_2", nullable = false)
    private Long userId2;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId1() { return userId1; }
    public void setUserId1(Long userId1) { this.userId1 = userId1; }
    public Long getUserId2() { return userId2; }
    public void setUserId2(Long userId2) { this.userId2 = userId2; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
