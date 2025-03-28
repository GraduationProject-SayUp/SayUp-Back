package com.sayup.SayUp.controller;

import com.sayup.SayUp.entity.ChatRoom;
import com.sayup.SayUp.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat-rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @PostMapping("/enter")
    public ChatRoom enterRoom(
            @RequestParam("currentUserId") Long currentUserId,
            @RequestParam("friendUserId") Long friendUserId
    ) throws Exception {
        return chatRoomService.createOrEnterRoom(currentUserId, friendUserId);
    }
}
