package com.FormFlow.FormFlow.Entity;

import com.FormFlow.FormFlow.enums.RoleType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "user_form_roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFormRole {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID userId;
    private UUID formId;

    @Enumerated(EnumType.STRING)
    private RoleType role;

    @Column(name = "is_viewed")
    private boolean isViewed = false;
}