package com.bingeboxed.social.repository;

import com.bingeboxed.social.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("SELECT f.userId2 FROM Friendship f WHERE f.userId1 = :userId " +
           "UNION SELECT f.userId1 FROM Friendship f WHERE f.userId2 = :userId")
    List<Long> findFriendIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(f) FROM Friendship f WHERE f.userId1 = :userId OR f.userId2 = :userId")
    long countFriendsByUserId(@Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN TRUE ELSE FALSE END FROM Friendship f " +
           "WHERE (f.userId1 = :userId1 AND f.userId2 = :userId2) OR (f.userId1 = :userId2 AND f.userId2 = :userId1)")
    boolean areFriends(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}
