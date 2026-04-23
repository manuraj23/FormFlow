package com.FormFlow.FormFlow.Repository;

import com.FormFlow.FormFlow.Entity.FormResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface FormResponseRepository extends JpaRepository<FormResponse, UUID> {

  //  List<FormResponse> findByFormId(UUID formId);
  // fixed — uses form relationship not formId directly
  List<FormResponse> findByForm_Id(UUID formId);



    // fixed — uses user relationship not response JSON
    List<FormResponse> findByUser_Email(String email);

    //long countByFormId(UUID formId);

    // fixed
    long countByForm_Id(UUID formId);

   // boolean existsByFormId(UUID formId);
   // fixed
   boolean existsByForm_Id(UUID formId);
    // count unique users who actually submitted a response
    @Query("SELECT COUNT(DISTINCT r.user.userId) FROM FormResponse r WHERE r.form.id = :formId AND r.user IS NOT NULL")
    long countDistinctRespondentsByFormId(UUID formId);

    // check if a specific user has already submitted a response for a form
    boolean existsByForm_IdAndUser_UserId(UUID formId, UUID userId);

}