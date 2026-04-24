package com.bingeboxed.social.repository;

import com.bingeboxed.social.entity.FriendRequest;
import com.bingeboxed.social.entity.FriendRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    List<FriendRequest> findByReceiverIdAndStatus(Long receiverId, FriendRequestStatus status);

    List<FriendRequest> findBySenderIdAndStatus(Long senderId, FriendRequestStatus status);

    Optional<FriendRequest> findBySenderIdAndReceiverId(Long senderId, Long receiverId);

    boolean existsBySenderIdAndReceiverIdAndStatus(Long senderId, Long receiverId, FriendRequestStatus status);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN TRUE ELSE FALSE END FROM FriendRequest f " +
           "WHERE ((f.senderId = :userId1 AND f.receiverId = :userId2) OR (f.senderId = :userId2 AND f.receiverId = :userId1)) " +
           "AND f.status = :status")
    boolean existsPendingRequestBetween(@Param("userId1") Long userId1, @Param("userId2") Long userId2, @Param("status") FriendRequestStatus status);
    
    // Count method for pending requests
    default long countByReceiverIdAndStatus(Long receiverId, FriendRequestStatus status) {
        return findByReceiverIdAndStatus(receiverId, status).size();
    }
}
