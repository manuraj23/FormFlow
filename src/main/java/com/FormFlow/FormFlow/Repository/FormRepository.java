package com.FormFlow.FormFlow.Repository;

import com.FormFlow.FormFlow.Entity.Form;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormRepository extends JpaRepository<Form, Long> {
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FormRepository extends JpaRepository<Form, Long> {

    // finds all forms where published = true or false
    // Spring Data JPA reads this method name and builds the query automatically
    List<Form> findByPublished(boolean published);
}