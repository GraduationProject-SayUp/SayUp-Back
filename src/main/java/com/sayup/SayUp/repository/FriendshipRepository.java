package com.sayup.SayUp.repository;

import com.sayup.SayUp.entity.FriendRelationship;
import com.sayup.SayUp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<FriendRelationship, Long> {
    List<FriendRelationship> findByRequesterAndStatus(User requester, FriendRelationship.FriendshipStatus status);
    List<FriendRelationship> findByAddresseeAndStatus(User addressee, FriendRelationship.FriendshipStatus status);

    @Query("SELECT f FROM FriendRelationship f WHERE " +
            "(f.requester = :user1 AND f.addressee = :user2) OR " +
            "(f.requester = :user2 AND f.addressee = :user1)")
    Optional<FriendRelationship> findRelationship(User user1, User user2);

    @Query("SELECT f FROM FriendRelationship f WHERE " +
            "((f.requester = :user OR f.addressee = :user) AND f.status = 'ACCEPTED')")
    List<FriendRelationship> findAllFriends(User user);
}
