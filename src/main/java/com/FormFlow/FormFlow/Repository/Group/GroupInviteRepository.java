package com.FormFlow.FormFlow.Repository.Group;

import com.FormFlow.FormFlow.Entity.GroupEntity.GroupInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GroupInviteRepository extends JpaRepository<GroupInvite, UUID> {
    Optional<GroupInvite> findByToken(String token);
}
