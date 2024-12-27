package com.sayup.SayUp.controller;

import com.sayup.SayUp.dto.SearchDTO;
import com.sayup.SayUp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/search")
    public ResponseEntity<?> searchUserId(
            @RequestParam(required = false) String searchTerm,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = userService.findUserIdByEmailOrUsername(searchTerm);
            return ResponseEntity.ok().body(new SearchDTO(userId));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}