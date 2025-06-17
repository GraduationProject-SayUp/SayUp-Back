package com.sayup.SayUp.dto.friend;

import com.sayup.SayUp.entity.user.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PendingRequestDTO {
    private Long relationshipId;
    private User requester;
}
