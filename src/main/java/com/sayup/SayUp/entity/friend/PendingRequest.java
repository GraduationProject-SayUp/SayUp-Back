package com.sayup.SayUp.entity.friend;

import com.sayup.SayUp.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "PendingRequest")
@Getter
@Setter
public class PendingRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "requesterId", nullable = false)
    private User requester;

    @ManyToOne
    @JoinColumn(name = "receiverId", nullable = false)
    private User receiver;

    public enum Status {
        PENDING, ACCEPTED, REJECTED
    }
}
