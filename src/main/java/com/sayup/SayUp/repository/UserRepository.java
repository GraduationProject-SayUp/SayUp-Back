package com.sayup.SayUp.repository;

import com.sayup.SayUp.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByEmailAndIsActiveTrue(String email);

    // email 또는 username 중 하나라도 일치하는 유저 조회
    @Query("SELECT u FROM User u WHERE (:email IS NULL OR u.email = :email) AND (:username IS NULL OR u.username = :username)")
    List<User> findByEmailOrUsername(@Param("email") String email, @Param("username") String username);

    // 활성 사용자만 조회
    @Query("SELECT u FROM User u WHERE u.isActive = true")
    List<User> findAllActiveUsers();
}
