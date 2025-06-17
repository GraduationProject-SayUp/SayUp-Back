package com.sayup.SayUp.entity.friend;

import com.sayup.SayUp.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "Friendship")
@Getter
@Setter
public class Friendship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "user1Id", nullable = false)
    private User user1;

    @ManyToOne
    @JoinColumn(name = "user2Id", nullable = false)
    private User user2;
}
