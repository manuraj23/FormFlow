package com.FormFlow.FormFlow.Repository;

import com.FormFlow.FormFlow.Entity.Form;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormRepository extends JpaRepository<Form, Long> {
}