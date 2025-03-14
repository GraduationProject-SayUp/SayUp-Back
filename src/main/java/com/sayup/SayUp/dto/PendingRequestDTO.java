package com.sayup.SayUp.dto;

import com.sayup.SayUp.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PendingRequestDTO {
    private Long relationshipId;
    private User requester;
}
