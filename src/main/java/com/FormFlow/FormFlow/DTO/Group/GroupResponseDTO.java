package com.FormFlow.FormFlow.DTO.Group;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class GroupResponseDTO {
    private UUID groupId;
    private String groupName;
    private String description;
    private String imageUrl;
    private Integer maxMembers;
    private Boolean isPrivate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private UUID ownerId;
    private String ownerUsername;
    private String ownerEmail;
}

