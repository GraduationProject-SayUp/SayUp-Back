package com.sayup.SayUp.repository;

import com.sayup.SayUp.entity.friend.FriendRelationship;
import com.sayup.SayUp.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<FriendRelationship, Long> {
    // 특정 유저에게 온 친구 요청 중 특정 상태인 관계 조회
    List<FriendRelationship> findByAddresseeAndStatus(User addressee, FriendRelationship.FriendshipStatus status);

    // 두 유저 간 친구 관계 조회
    @Query("SELECT f FROM FriendRelationship f WHERE " +
            "(f.requester = :user1 AND f.addressee = :user2) OR " +
            "(f.requester = :user2 AND f.addressee = :user1)")
    Optional<FriendRelationship> findRelationship(User user1, User user2);

    // user의 친구 목록 조회
    @Query("SELECT f FROM FriendRelationship f WHERE " +
            "((f.requester = :user OR f.addressee = :user) AND f.status = 'ACCEPTED')")
    List<FriendRelationship> findAllFriends(User user);
}
