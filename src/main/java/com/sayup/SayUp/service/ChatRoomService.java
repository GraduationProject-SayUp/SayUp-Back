package com.sayup.SayUp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sayup.SayUp.entity.ChatRoom;
import com.sayup.SayUp.entity.User;
import com.sayup.SayUp.repository.ChatRoomRepository;
import com.sayup.SayUp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * 두 사용자가 참여하는 채팅방이 이미 존재하면 해당 방을 반환
     * 없으면 새 채팅방을 생성하여 반환
     *
     * @param currentUserId 현재 로그인한 유저의 ID
     * @param friendUserId 친구로 선택한 유저의 ID
     * @return 기존 또는 새로 생성된 채팅방
     * @throws Exception JSON 변환 실패 등 예외 발생 시
     */
    public ChatRoom createOrEnterRoom(Long currentUserId, Long friendUserId) throws Exception {
        // 두 사용자 간의 기존 채팅방이 존재하는지 확인
        Optional<ChatRoom> existingRoom = chatRoomRepository.findByUserIds(currentUserId, friendUserId);
        if (existingRoom.isPresent()) return existingRoom.get();

        // 사용자 정보 조회 (존재하지 않으면 예외 발생)
        User currentUser = userRepository.findById(currentUserId).orElseThrow();
        User friendUser = userRepository.findById(friendUserId).orElseThrow();

        // 친구의 TTS 벡터를 메타데이터에 저장 (key: tts_vector_{friendUserId})
        Map<String, Object> metadataMap = new HashMap<>();
        metadataMap.put("tts_vector_" + friendUserId, friendUser.getTtsVector());

        String metadataJson = objectMapper.writeValueAsString(metadataMap);

        // 새로운 채팅방 객체 생성 및 저장
        ChatRoom room = ChatRoom.builder()
                .participants(Arrays.asList(currentUser, friendUser))
                .metadata(metadataJson)
                .build();

        return chatRoomRepository.save(room);
    }
}
