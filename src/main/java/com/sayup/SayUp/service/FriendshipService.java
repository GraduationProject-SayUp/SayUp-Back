package com.sayup.SayUp.service;

import com.sayup.SayUp.controller.FriendshipController;
import com.sayup.SayUp.dto.PendingRequestDTO;
import com.sayup.SayUp.entity.FriendRelationship;
import com.sayup.SayUp.entity.User;
import com.sayup.SayUp.repository.FriendshipRepository;
import com.sayup.SayUp.repository.UserRepository;
import com.sayup.SayUp.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FriendshipService {
    private static final Logger logger = LoggerFactory.getLogger(FriendshipController.class);

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * @param addresseeId: 친구 요청을 받을 사용자의 userId
     */
    @Transactional
    public void sendFriendRequest(CustomUserDetails requesterDetails, Integer addresseeId) {
        User requester = requesterDetails.getUser(); // User 객체 직접 접근
        User addressee = userRepository.findById(addresseeId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 자기 자신에게 친구 요청을 보내는 것을 방지
        if (requester.getUserId().equals(addresseeId.longValue())) {
            throw new RuntimeException("Cannot send friend request to yourself");
        }

        // 이미 존재하는 친구 관계 검증
        Optional<FriendRelationship> existingRelationship =
                friendshipRepository.findRelationship(requester, addressee);

        if (existingRelationship.isPresent()) {
            FriendRelationship relationship = existingRelationship.get();
            if (relationship.getStatus() == FriendRelationship.FriendshipStatus.ACCEPTED) {
                throw new RuntimeException("Already friends");
            } else if (relationship.getStatus() == FriendRelationship.FriendshipStatus.PENDING) {
                throw new RuntimeException("Friend request already pending");
            } else if (relationship.getStatus() == FriendRelationship.FriendshipStatus.REJECTED) {
                // 거절된 요청의 경우 새로운 요청을 허용하되, 이전 요청을 삭제
                friendshipRepository.delete(relationship);
            }
        }

        FriendRelationship relationship = new FriendRelationship();
        relationship.setRequester(requester);
        relationship.setAddressee(addressee);
        relationship.setStatus(FriendRelationship.FriendshipStatus.PENDING);
        relationship.setRequestedAt(LocalDateTime.now());

        friendshipRepository.save(relationship);
    }

    /**
     * 전체 흐름 정리
     * 1. A 유저가 B에게 친구 요청 
     *    서버에서 FriendRelationship 생성 -> relationshipId 생성
     * 2. B 유저가 getPendingRequests() API 호출
     *    B를 향한 PENDING 상태의 요청들을 조회
     *    각 요청마다 relationshipId + 요청자 정보 반환
     * 3. B 유저가 특정 요청을 수락
     *    /friend/accept API 호출 시 relationshipId 전달
     *    서버에서 해당 요청의 상태를 ACCEPTED로 변경
     */
    @Transactional
    public void acceptFriendRequest(CustomUserDetails addresseeDetails, Long relationshipId) {
        User addressee = addresseeDetails.getUser();
        FriendRelationship relationship = friendshipRepository.findById(relationshipId)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));

        if (!relationship.getAddressee().equals(addressee)) {
            throw new RuntimeException("Not authorized to accept this request");
        }

        if (relationship.getStatus() != FriendRelationship.FriendshipStatus.PENDING) {
            throw new RuntimeException("Request is not in PENDING status");
        }

        relationship.setStatus(FriendRelationship.FriendshipStatus.ACCEPTED);
        relationship.setAcceptedAt(LocalDateTime.now());

        friendshipRepository.save(relationship);
    }

    public List<User> getFriendsList(CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        return friendshipRepository.findAllFriends(user).stream()
                .map(relationship ->
                        relationship.getRequester().equals(user)
                                ? relationship.getAddressee()
                                : relationship.getRequester())
                .collect(Collectors.toList());
    }

    public List<PendingRequestDTO> getPendingRequests(CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        List<FriendRelationship> relationships = friendshipRepository.findByAddresseeAndStatus(user, FriendRelationship.FriendshipStatus.PENDING);
        logger.info("Finding pending requests for user: {}, results: {}", user.getUserId(), relationships.size());

        return relationships.stream()
                .map(rel -> new PendingRequestDTO(rel.getId(), rel.getRequester()))
                .collect(Collectors.toList());
    }
}
