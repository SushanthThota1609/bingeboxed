package com.bingeboxed.social.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "friendships",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_friendship_pair",
        columnNames = {"user_id_1", "user_id_2"}
    )
)
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id_1", nullable = false)
    private Long userId1;

    @Column(name = "user_id_2", nullable = false)
    private Long userId2;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId1() { return userId1; }
    public void setUserId1(Long userId1) { this.userId1 = userId1; }

    public Long getUserId2() { return userId2; }
    public void setUserId2(Long userId2) { this.userId2 = userId2; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
