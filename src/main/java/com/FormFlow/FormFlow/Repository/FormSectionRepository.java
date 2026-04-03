package com.FormFlow.FormFlow.Repository;

import com.FormFlow.FormFlow.Entity.FormSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FormSectionRepository extends JpaRepository<FormSection, Long> {

    @Query("""
        SELECT DISTINCT s FROM FormSection s
        LEFT JOIN FETCH s.fields
        WHERE s.form.id IN :formIds
    """)
    List<FormSection> findByFormIdInWithFields(@Param("formIds") List<Long> formIds);


}

