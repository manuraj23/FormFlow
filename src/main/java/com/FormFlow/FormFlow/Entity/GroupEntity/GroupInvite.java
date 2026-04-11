package com.FormFlow.FormFlow.Entity.GroupEntity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "group_invites")
@Data
public class GroupInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID inviteId;

    @Column(unique = true, nullable = false)
    private String token;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    private LocalDateTime expiryTime;
    private boolean isActive = true;
    private LocalDateTime createdAt;
}
