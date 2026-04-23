package com.bingeboxed.social.repository;

import com.bingeboxed.social.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("""
        SELECT f FROM Friendship f
        WHERE f.userId1 = :userId OR f.userId2 = :userId
        """)
    List<Friendship> findAllByUserId(@Param("userId") Long userId);

    @Query("""
        SELECT COUNT(f) FROM Friendship f
        WHERE f.userId1 = :userId OR f.userId2 = :userId
        """)
    long countByUserId(@Param("userId") Long userId);

    @Query("""
        SELECT COUNT(f) > 0 FROM Friendship f
        WHERE (f.userId1 = :userA AND f.userId2 = :userB)
           OR (f.userId1 = :userB AND f.userId2 = :userA)
        """)
    boolean existsBetweenUsers(@Param("userA") Long userA, @Param("userB") Long userB);

    @Query("""
        SELECT CASE WHEN f.userId1 = :userId THEN f.userId2 ELSE f.userId1 END
        FROM Friendship f
        WHERE f.userId1 = :userId OR f.userId2 = :userId
        """)
    List<Long> findFriendIdsByUserId(@Param("userId") Long userId);
}
