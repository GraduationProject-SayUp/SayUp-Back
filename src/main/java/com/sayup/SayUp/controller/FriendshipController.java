package com.sayup.SayUp.controller;

import com.sayup.SayUp.dto.PendingRequestDTO;
import com.sayup.SayUp.entity.User;
import com.sayup.SayUp.security.CustomUserDetails;
import com.sayup.SayUp.service.FriendshipService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
@AllArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;

    @PostMapping("/request/{addresseeId}")
    public ResponseEntity<?> sendFriendRequest(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long addresseeId) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        friendshipService.sendFriendRequest((CustomUserDetails) userDetails, addresseeId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/accept/{relationshipId}")
    public ResponseEntity<?> acceptFriendRequest(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long relationshipId) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        friendshipService.acceptFriendRequest((CustomUserDetails) userDetails, relationshipId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/list")
    public ResponseEntity<List<User>> getFriendsList(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(friendshipService.getFriendsList((CustomUserDetails) userDetails));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<PendingRequestDTO>> getPendingRequests(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(friendshipService.getPendingRequests(userDetails));
    }
}
