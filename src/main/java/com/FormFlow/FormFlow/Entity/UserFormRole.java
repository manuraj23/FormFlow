package com.FormFlow.FormFlow.Entity;

import com.FormFlow.FormFlow.enums.RoleType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
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

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "form_id", nullable = false)
    private Form form;

    @Enumerated(EnumType.STRING)
    private RoleType role;

    @Column(name = "is_viewed")
    private boolean isViewed = false;

    @Column(name = "message")
    private String message;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;
}