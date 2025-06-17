package com.sayup.SayUp.service.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sayup.SayUp.entity.chat.ChatRoom;
import com.sayup.SayUp.entity.user.User;
import com.sayup.SayUp.repository.ChatRoomRepository;
import com.sayup.SayUp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * 두 사용자가 참여하는 채팅방 생성 또는 기존 방 반환
     *
     * @param currentUserId 현재 로그인한 유저의 ID
     * @param friendUserId 친구로 선택한 유저의 ID
     * @return 기존 또는 새로 생성된 채팅방
     */
    public ChatRoom createOrEnterRoom(Long currentUserId, Long friendUserId) {
        // 입력 검증
        if (currentUserId == null || friendUserId == null) {
            throw new IllegalArgumentException("사용자 ID는 null일 수 없습니다.");
        }

        if (currentUserId.equals(friendUserId)) {
            throw new IllegalArgumentException("자기 자신과는 채팅방을 만들 수 없습니다.");
        }

        log.info("Creating or entering chat room between users: {} and {}", currentUserId, friendUserId);

        // 두 사용자 간의 기존 채팅방이 존재하는지 확인
        Optional<ChatRoom> existingRoom = chatRoomRepository.findByUserIds(currentUserId, friendUserId);
        if (existingRoom.isPresent()) {
            log.info("Existing chat room found: {}", existingRoom.get().getId());
            return existingRoom.get();
        }

        // 사용자 정보 조회 및 검증
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("현재 사용자를 찾을 수 없습니다. ID: " + currentUserId));
        
        User friendUser = userRepository.findById(friendUserId)
                .orElseThrow(() -> new IllegalArgumentException("친구 사용자를 찾을 수 없습니다. ID: " + friendUserId));

        // 사용자 활성 상태 확인
        if (!currentUser.getIsActive() || !friendUser.getIsActive()) {
            throw new IllegalArgumentException("비활성 사용자와는 채팅방을 만들 수 없습니다.");
        }

        try {
            // 친구의 TTS 벡터를 메타데이터에 저장 (암호화 고려 필요)
            Map<String, Object> metadataMap = new HashMap<>();
            if (friendUser.getTtsVector() != null && !friendUser.getTtsVector().trim().isEmpty()) {
                metadataMap.put("tts_vector_" + friendUserId, friendUser.getTtsVector());
                log.debug("TTS vector stored for user: {}", friendUserId);
            }

        String metadataJson = objectMapper.writeValueAsString(metadataMap);

        // 새로운 채팅방 객체 생성 및 저장
        ChatRoom room = ChatRoom.builder()
                .participants(Arrays.asList(currentUser, friendUser))
                .metadata(metadataJson)
                .build();

            ChatRoom savedRoom = chatRoomRepository.save(room);
            log.info("New chat room created: {} between users: {} and {}", 
                    savedRoom.getId(), currentUserId, friendUserId);
            
            return savedRoom;

        } catch (Exception e) {
            log.error("Error creating chat room between users {} and {}: {}", 
                    currentUserId, friendUserId, e.getMessage());
            throw new IllegalStateException("채팅방 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 사용자가 참여한 채팅방 목록 조회
     */
    @Transactional(readOnly = true)
    public java.util.List<ChatRoom> getUserChatRooms(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 null일 수 없습니다.");
        }

        log.info("Fetching chat rooms for user: {}", userId);
        return chatRoomRepository.findByUserId(userId);
    }

    /**
     * 채팅방 정보 조회
     */
    @Transactional(readOnly = true)
    public ChatRoom getChatRoom(Long roomId, Long userId) {
        if (roomId == null || userId == null) {
            throw new IllegalArgumentException("채팅방 ID와 사용자 ID는 null일 수 없습니다.");
        }

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다. ID: " + roomId));

        // 사용자가 해당 채팅방의 참여자인지 확인
        boolean isParticipant = room.getParticipants().stream()
                .anyMatch(user -> user.getUserId().equals(userId));

        if (!isParticipant) {
            log.warn("User {} attempted to access chat room {} without permission", userId, roomId);
            throw new IllegalArgumentException("해당 채팅방에 접근할 권한이 없습니다.");
        }

        return room;
    }
}
