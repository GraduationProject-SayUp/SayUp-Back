package com.sayup.SayUp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "FriendRelationship")
@Getter
@Setter
public class FriendRelationship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne
    @JoinColumn(name = "addressee_id", nullable = false)
    private User addressee;

    @Enumerated(EnumType.STRING)
    private FriendshipStatus status;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime acceptedAt;

    public enum FriendshipStatus {
        PENDING,    // 친구 요청이 보내진 상태
        ACCEPTED,   // 친구 요청이 수락된 상태
        REJECTED    // 친구 요청이 거절된 상태
    }
}