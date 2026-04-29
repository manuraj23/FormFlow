package com.FormFlow.FormFlow.Repository;

import com.FormFlow.FormFlow.Entity.UserFormRole;
import com.FormFlow.FormFlow.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserFormRoleRepository extends JpaRepository<UserFormRole, UUID> {

    List<UserFormRole> findByFormId(UUID formId);

    void deleteByFormId(UUID formId);

    List<UserFormRole> findByUser_UserIdAndIsViewedFalseAndForm_IsDeletedFalse(UUID userId);
    List<UserFormRole> findByUser_UserIdAndIsViewedTrueAndForm_IsDeletedFalse(UUID userId);
    Optional<UserFormRole> findByUser_UsernameAndForm_Id(String username, UUID formId);
//    List<UserFormRole> findByUser_UserIdAndIsViewedFalse(UUID userId);
//    List<UserFormRole> findByUser_UserIdAndIsViewedTrue(UUID userId);
// count unique users assigned as RESPONDER to a form
@Query("SELECT COUNT(DISTINCT r.user.userId) FROM UserFormRole r WHERE r.form.id = :formId AND r.role = :role")
long countDistinctAssigneesByFormIdAndRole(UUID formId, RoleType role);

    List<UserFormRole> findByFormIdAndRole(UUID formId, RoleType role);

}