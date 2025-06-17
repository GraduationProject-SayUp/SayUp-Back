package com.sayup.SayUp.service.friend;

import com.sayup.SayUp.dto.friend.PendingRequestDTO;
import com.sayup.SayUp.entity.friend.FriendRelationship;
import com.sayup.SayUp.entity.user.User;
import com.sayup.SayUp.repository.FriendshipRepository;
import com.sayup.SayUp.repository.UserRepository;
import com.sayup.SayUp.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    /**
     * 친구 요청 보내기
     * 
     * @param requesterDetails 요청자 정보
     * @param addresseeId 친구 요청을 받을 사용자의 userId
     */
    public void sendFriendRequest(CustomUserDetails requesterDetails, Long addresseeId) {
        // 입력 검증
        if (requesterDetails == null || addresseeId == null) {
            throw new IllegalArgumentException("요청자 정보와 대상 사용자 ID는 null일 수 없습니다.");
        }

        User requester = requesterDetails.getUser();

        // 자기 자신에게 친구 요청을 보내는 것을 방지
        if (requester.getUserId().equals(addresseeId)) {
            log.warn("User {} attempted to send friend request to themselves", requester.getUserId());
            throw new IllegalArgumentException("자기 자신에게는 친구 요청을 보낼 수 없습니다.");
        }

        // 대상 사용자 존재 확인
        User addressee = userRepository.findById(addresseeId)
                .orElseThrow(() -> new IllegalArgumentException("대상 사용자를 찾을 수 없습니다. ID: " + addresseeId));

        // 사용자 활성 상태 확인
        if (!requester.getIsActive() || !addressee.getIsActive()) {
            throw new IllegalArgumentException("비활성 사용자와는 친구 관계를 맺을 수 없습니다.");
        }

        log.info("Friend request from user {} to user {}", requester.getUserId(), addresseeId);

        // 이미 존재하는 친구 관계 검증
        Optional<FriendRelationship> existingRelationship =
                friendshipRepository.findRelationship(requester, addressee);

        if (existingRelationship.isPresent()) {
            FriendRelationship relationship = existingRelationship.get();
            
            if (relationship.getStatus() == FriendRelationship.FriendshipStatus.ACCEPTED) {
                log.warn("Friend request failed: Already friends between {} and {}", 
                        requester.getUserId(), addresseeId);
                throw new IllegalArgumentException("이미 친구 관계입니다.");
            } else if (relationship.getStatus() == FriendRelationship.FriendshipStatus.PENDING) {
                log.warn("Friend request failed: Request already pending between {} and {}", 
                        requester.getUserId(), addresseeId);
                throw new IllegalArgumentException("이미 대기 중인 친구 요청이 있습니다.");
            } else if (relationship.getStatus() == FriendRelationship.FriendshipStatus.REJECTED) {
                // 거절된 요청의 경우 새로운 요청을 허용하되, 이전 요청을 삭제
                log.info("Deleting previous rejected request and creating new one between {} and {}", 
                        requester.getUserId(), addresseeId);
                friendshipRepository.delete(relationship);
            }
        }

        // 새로운 친구 요청 생성
        FriendRelationship relationship = new FriendRelationship();
        relationship.setRequester(requester);
        relationship.setAddressee(addressee);
        relationship.setStatus(FriendRelationship.FriendshipStatus.PENDING);
        relationship.setRequestedAt(LocalDateTime.now());

        friendshipRepository.save(relationship);
        log.info("Friend request created successfully between {} and {}", 
                requester.getUserId(), addresseeId);
    }

    /**
     * 친구 요청 수락
     * 
     * @param addresseeDetails 요청을 받은 사용자 정보
     * @param relationshipId 친구 관계 ID
     */
    public void acceptFriendRequest(CustomUserDetails addresseeDetails, Long relationshipId) {
        // 입력 검증
        if (addresseeDetails == null || relationshipId == null) {
            throw new IllegalArgumentException("사용자 정보와 관계 ID는 null일 수 없습니다.");
        }

        User addressee = addresseeDetails.getUser();
        
        log.info("Friend request acceptance attempt by user {} for relationship {}", 
                addressee.getUserId(), relationshipId);

        // 친구 관계 조회
        FriendRelationship relationship = friendshipRepository.findById(relationshipId)
                .orElseThrow(() -> new IllegalArgumentException("친구 요청을 찾을 수 없습니다. ID: " + relationshipId));

        // 권한 검증: 요청을 받은 사용자가 맞는지 확인
        if (!relationship.getAddressee().getUserId().equals(addressee.getUserId())) {
            log.warn("Unauthorized friend request acceptance attempt by user {} for relationship {}", 
                    addressee.getUserId(), relationshipId);
            throw new IllegalArgumentException("해당 친구 요청을 수락할 권한이 없습니다.");
        }

        // 상태 검증
        if (relationship.getStatus() != FriendRelationship.FriendshipStatus.PENDING) {
            log.warn("Invalid friend request status for acceptance: {} (relationship: {})", 
                    relationship.getStatus(), relationshipId);
            throw new IllegalArgumentException("대기 중인 요청만 수락할 수 있습니다.");
        }

        // 친구 요청 수락 처리
        relationship.setStatus(FriendRelationship.FriendshipStatus.ACCEPTED);
        relationship.setAcceptedAt(LocalDateTime.now());

        friendshipRepository.save(relationship);
        log.info("Friend request accepted successfully: {} by user {}", 
                relationshipId, addressee.getUserId());
    }

    /**
     * 친구 요청 거절
     */
    public void rejectFriendRequest(CustomUserDetails addresseeDetails, Long relationshipId) {
        // 입력 검증
        if (addresseeDetails == null || relationshipId == null) {
            throw new IllegalArgumentException("사용자 정보와 관계 ID는 null일 수 없습니다.");
        }

        User addressee = addresseeDetails.getUser();
        
        log.info("Friend request rejection attempt by user {} for relationship {}", 
                addressee.getUserId(), relationshipId);

        // 친구 관계 조회
        FriendRelationship relationship = friendshipRepository.findById(relationshipId)
                .orElseThrow(() -> new IllegalArgumentException("친구 요청을 찾을 수 없습니다. ID: " + relationshipId));

        // 권한 검증
        if (!relationship.getAddressee().getUserId().equals(addressee.getUserId())) {
            log.warn("Unauthorized friend request rejection attempt by user {} for relationship {}", 
                    addressee.getUserId(), relationshipId);
            throw new IllegalArgumentException("해당 친구 요청을 거절할 권한이 없습니다.");
        }

        // 상태 검증
        if (relationship.getStatus() != FriendRelationship.FriendshipStatus.PENDING) {
            throw new IllegalArgumentException("대기 중인 요청만 거절할 수 있습니다.");
        }

        // 친구 요청 거절 처리
        relationship.setStatus(FriendRelationship.FriendshipStatus.REJECTED);
        friendshipRepository.save(relationship);
        
        log.info("Friend request rejected successfully: {} by user {}", 
                relationshipId, addressee.getUserId());
    }

    /**
     * 친구 목록 조회
     */
    @Transactional(readOnly = true)
    public List<User> getFriendsList(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("사용자 정보는 null일 수 없습니다.");
        }

        User user = userDetails.getUser();
        log.info("Fetching friends list for user: {}", user.getUserId());

        return friendshipRepository.findAllFriends(user).stream()
                .map(relationship ->
                        relationship.getRequester().getUserId().equals(user.getUserId())
                                ? relationship.getAddressee()
                                : relationship.getRequester())
                .filter(friend -> friend.getIsActive()) // 활성 사용자만 필터링
                .collect(Collectors.toList());
    }

    /**
     * 대기 중인 친구 요청 목록 조회
     */
    @Transactional(readOnly = true)
    public List<PendingRequestDTO> getPendingRequests(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("사용자 정보는 null일 수 없습니다.");
        }

        User user = userDetails.getUser();
        log.info("Fetching pending friend requests for user: {}", user.getUserId());

        List<FriendRelationship> relationships = friendshipRepository
                .findByAddresseeAndStatus(user, FriendRelationship.FriendshipStatus.PENDING);

        List<PendingRequestDTO> pendingRequests = relationships.stream()
                .filter(rel -> rel.getRequester().getIsActive()) // 활성 사용자의 요청만 필터링
                .map(rel -> new PendingRequestDTO(rel.getId(), rel.getRequester()))
                .collect(Collectors.toList());

        log.info("Found {} pending requests for user: {}", pendingRequests.size(), user.getUserId());
        return pendingRequests;
    }

    /**
     * 친구 관계 삭제
     */
    public void removeFriend(CustomUserDetails userDetails, Long friendUserId) {
        if (userDetails == null || friendUserId == null) {
            throw new IllegalArgumentException("사용자 정보와 친구 ID는 null일 수 없습니다.");
        }

        User user = userDetails.getUser();
        
        log.info("Friend removal attempt by user {} for friend {}", user.getUserId(), friendUserId);

        // 친구 관계 조회
        Optional<FriendRelationship> relationship = friendshipRepository.findRelationship(user, 
                userRepository.findById(friendUserId)
                        .orElseThrow(() -> new IllegalArgumentException("친구 사용자를 찾을 수 없습니다. ID: " + friendUserId)));

        if (relationship.isEmpty()) {
            throw new IllegalArgumentException("친구 관계가 존재하지 않습니다.");
        }

        FriendRelationship rel = relationship.get();
        if (rel.getStatus() != FriendRelationship.FriendshipStatus.ACCEPTED) {
            throw new IllegalArgumentException("친구 관계가 아닙니다.");
        }

        // 친구 관계 삭제
        friendshipRepository.delete(rel);
        log.info("Friend relationship removed successfully between {} and {}", 
                user.getUserId(), friendUserId);
    }
}
