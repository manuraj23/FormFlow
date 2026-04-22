package com.FormFlow.FormFlow.Repository;

import com.FormFlow.FormFlow.Entity.Template.FormTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FormTemplateRepository extends JpaRepository<FormTemplate, UUID> {
    List<FormTemplate> findByActiveTrueOrderByTemplateNameAsc();

    Optional<FormTemplate> findByTemplateName(String templateName);
}

