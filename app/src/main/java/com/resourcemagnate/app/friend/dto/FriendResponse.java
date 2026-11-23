package com.resourcemagnate.app.friend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendResponse {
    private UUID id;
    private UUID userId;
    private UUID friendId;
    private String status;
    private String displayName;
    private String email;
    private Long score;
    private Integer rank;
}