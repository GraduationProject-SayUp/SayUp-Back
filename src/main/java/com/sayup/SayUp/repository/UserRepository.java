package com.sayup.SayUp.repository;

import com.sayup.SayUp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE (:email IS NOT NULL AND u.email = :email) OR (:username IS NOT NULL AND u.username = :username)")
    List<User> findByEmailOrUsername(@Param("email") String email, @Param("username") String username);
}
