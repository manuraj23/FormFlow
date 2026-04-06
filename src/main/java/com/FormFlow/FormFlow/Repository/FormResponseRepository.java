package com.FormFlow.FormFlow.Repository;

import com.FormFlow.FormFlow.Entity.FormResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface FormResponseRepository extends JpaRepository<FormResponse, UUID> {

    List<FormResponse> findByFormId(UUID formId);

    @Query(value = "SELECT * FROM form_responses WHERE response ->> 'Email' = :email", nativeQuery = true)
    List<FormResponse> findByEmail(String email);

    long countByFormId(UUID formId);

    boolean existsByFormId(UUID formId);
}