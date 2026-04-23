package com.bingeboxed.social.repository;

import com.bingeboxed.social.entity.FriendRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    List<FriendRequest> findByReceiverIdAndStatus(Long receiverId, FriendRequest.Status status);

    List<FriendRequest> findBySenderIdAndStatus(Long senderId, FriendRequest.Status status);

    Optional<FriendRequest> findBySenderIdAndReceiverId(Long senderId, Long receiverId);

    @Query("""
        SELECT COUNT(fr) FROM FriendRequest fr
        WHERE fr.receiverId = :userId AND fr.status = 'PENDING'
        """)
    long countPendingRequestsForUser(@Param("userId") Long userId);

    @Query("""
        SELECT fr FROM FriendRequest fr
        WHERE (fr.senderId = :userA AND fr.receiverId = :userB)
           OR (fr.senderId = :userB AND fr.receiverId = :userA)
        """)
    Optional<FriendRequest> findBetweenUsers(@Param("userA") Long userA, @Param("userB") Long userB);
}
