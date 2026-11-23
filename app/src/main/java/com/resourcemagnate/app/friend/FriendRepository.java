package com.resourcemagnate.app.friend;

import com.resourcemagnate.app.friend.FriendStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendRepository extends JpaRepository<Friend, UUID> {

    List<Friend> findByUserIdAndStatus(UUID userId, FriendStatus status);

    List<Friend> findByFriendIdAndStatus(UUID friendId, FriendStatus status);

    Optional<Friend> findByUserIdAndFriendId(UUID userId, UUID friendId);

    Optional<Friend> findByFriendIdAndUserId(UUID friendId, UUID userId);

    boolean existsByUserIdAndFriendId(UUID userId, UUID friendId);

    boolean existsByFriendIdAndUserId(UUID friendId, UUID userId);

    @Modifying
    @Transactional
    @Query("UPDATE Friend f SET f.status = :status WHERE f.id = :id")
    int updateStatus(@Param("id") UUID id, @Param("status") FriendStatus status);

    List<Friend> findByStatus(FriendStatus status);

    long countByStatus(FriendStatus status);
}