package com.sayup.SayUp.repository;

import com.sayup.SayUp.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 두 명의 유저가 모두 참여하고 있는 방을 찾기
    @Query("SELECT r FROM ChatRoom r JOIN r.participants p1 JOIN r.participants p2 WHERE p1.userId = :userId1 AND p2.userId = :userId2")
    Optional<ChatRoom> findByUserIds(@Param("userId1") Long userId1,
                                     @Param("userId2") Long userId2
    );

    // 특정 사용자가 참여한 채팅방 목록 조회
    @Query("SELECT DISTINCT r FROM ChatRoom r JOIN r.participants p WHERE p.userId = :userId")
    List<ChatRoom> findByUserId(@Param("userId") Long userId);

    // 채팅방 참여자 수로 채팅방 조회 (1:1 채팅방만)
    @Query("SELECT r FROM ChatRoom r JOIN r.participants p GROUP BY r HAVING COUNT(p) = 2")
    List<ChatRoom> findOneToOneChatRooms();
}
