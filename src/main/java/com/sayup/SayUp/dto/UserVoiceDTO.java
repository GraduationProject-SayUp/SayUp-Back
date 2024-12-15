package com.sayup.SayUp.dto;

import com.sayup.SayUp.entity.User;
import lombok.Data;

@Data
public class UserVoiceDTO {
    private Long id;

    private User user;

    private String fileName;

    private String filePath;
}
