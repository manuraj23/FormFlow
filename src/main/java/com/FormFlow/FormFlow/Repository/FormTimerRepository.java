package com.FormFlow.FormFlow.Repository;

import com.FormFlow.FormFlow.Entity.FormTimer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FormTimerRepository extends JpaRepository<FormTimer, UUID> {

    Optional<FormTimer> findByForm_IdAndUser_UserId(
            UUID formId,
            UUID userId
    );
    Optional<FormTimer>
    findTopByForm_IdAndUser_UserIdOrderByStartTimeDesc(
            UUID formId,
            UUID userId
    );
}