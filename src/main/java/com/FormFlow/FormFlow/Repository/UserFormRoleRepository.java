package com.FormFlow.FormFlow.Repository;

import com.FormFlow.FormFlow.Entity.UserFormRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserFormRoleRepository extends JpaRepository<UserFormRole, UUID> {

    List<UserFormRole> findByFormId(UUID formId);

    void deleteByFormId(UUID formId);

    List<UserFormRole> findByUserIdAndIsViewedFalse(UUID userId);

    List<UserFormRole> findByUserIdAndIsViewedTrue(UUID userId);
}