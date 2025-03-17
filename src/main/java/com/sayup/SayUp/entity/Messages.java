package com.sayup.SayUp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "Messages")
@Getter
@Setter
public class Messages {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId;

    @ManyToOne
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    @Enumerated(EnumType.STRING)
    private Sender sender;

    @Column(name = "cntent", length = 255)
    private String content;

    private Float similarityScore;

    @Column(length = 255)
    private String ttsVectorUsed;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Sender {
        USER, AI
    }
}
