package com.FormFlow.FormFlow.Repository;

import com.FormFlow.FormFlow.Entity.FormTempTimer;
import com.FormFlow.FormFlow.Entity.FormTimer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FormTempTimerRepository extends JpaRepository<FormTempTimer, UUID> {

    Optional<FormTempTimer> findByForm_IdAndTempUserId(
            UUID formId,
            UUID tempUserId
    );

    Optional<FormTempTimer> findTopByForm_IdAndTempUserIdOrderByStartTimeDesc(
            UUID formId,
            UUID tempUserId
    );
}
