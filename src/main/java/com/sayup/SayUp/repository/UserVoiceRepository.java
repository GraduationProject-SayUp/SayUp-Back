package com.sayup.SayUp.repository;

import com.sayup.SayUp.entity.UserVoice;
import com.sayup.SayUp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserVoiceRepository extends JpaRepository<UserVoice,Long> {
    Optional<UserVoice> findByUser(User user);
}
