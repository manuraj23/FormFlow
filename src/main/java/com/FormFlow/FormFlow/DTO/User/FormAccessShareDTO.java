package com.FormFlow.FormFlow.DTO.User;

import com.FormFlow.FormFlow.enums.RoleType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class FormAccessShareDTO {

    private UUID formId;
    private String formName;
    private RoleType role;
    private LocalDateTime assignedAt;
    private String assignedBy;
    private String message;
    private boolean viewed;
}