package com.sayup.SayUp.controller;

import com.sayup.SayUp.dto.PendingRequestDTO;
import com.sayup.SayUp.entity.User;
import com.sayup.SayUp.security.CustomUserDetails;
import com.sayup.SayUp.service.FriendshipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
public class FriendshipController {

    @Autowired
    private FriendshipService friendshipService;

    private static final Logger logger = LoggerFactory.getLogger(FriendshipController.class);

    @PostMapping("/request/{addresseeId}")
    public ResponseEntity<?> sendFriendRequest(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Integer addresseeId) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        } else if (!(userDetails instanceof CustomUserDetails)) {
            logger.error("UserDetails is not an instance of CustomUserDetails");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error");
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
