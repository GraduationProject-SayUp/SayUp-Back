package com.sayup.SayUp.repository;

import com.sayup.SayUp.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 두 명의 유저가 모두 참여하고 있는 방을 찾기
    @Query("SELECT r FROM ChatRoom r JOIN r.participants p1 JOIN r.participants p2 WHERE p1.userId = :userId1 AND p2.userId = :userId2")
    Optional<ChatRoom> findByUserIds(@Param("userId1") Long userId1,
                                     @Param("userId2") Long userId2
    );
}
