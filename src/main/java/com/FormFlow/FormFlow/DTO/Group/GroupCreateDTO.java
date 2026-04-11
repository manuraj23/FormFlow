package com.FormFlow.FormFlow.DTO.Group;

import lombok.Data;

@Data
public class GroupCreateDTO {
    private String groupName;
    private String description;
    private Boolean isPrivate;
    private String imageUrl;
    private Integer maxMembers;
}
