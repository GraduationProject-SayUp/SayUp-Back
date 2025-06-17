package com.sayup.SayUp.repository;

import com.sayup.SayUp.entity.user.UserVoice;
import com.sayup.SayUp.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserVoiceRepository extends JpaRepository<UserVoice,Long> {
    Optional<UserVoice> findByUser(User user);
}
