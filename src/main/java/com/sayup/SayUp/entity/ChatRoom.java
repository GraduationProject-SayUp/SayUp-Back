package com.sayup.SayUp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "ChatRoom")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 채팅방에 참여하는 유저들
    @ManyToMany
    @JoinTable(
            name = "ChatRoom_Users",
            joinColumns = @JoinColumn(name = "chatroom_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private List<User> participants = new ArrayList<>();

    @Lob
    private String metadata; // TTS 벡터 등 JSON 문자열로 저장

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
