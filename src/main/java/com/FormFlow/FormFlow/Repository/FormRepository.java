package com.FormFlow.FormFlow.Repository;

import com.FormFlow.FormFlow.Entity.Form;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FormRepository extends JpaRepository<Form, Long> {

    // finds all forms where published = true or false
    // Spring Data JPA reads this method name and builds the query automatically
    List<Form> findByPublished(boolean published);
    @Query("""
        SELECT DISTINCT f FROM Form f
        LEFT JOIN FETCH f.sections s
        WHERE f.user.username = :username
    """)
    List<Form> findFormsByUsernameWithSections(@Param("username") String username);


    @Query("""
    SELECT DISTINCT f FROM Form f
    LEFT JOIN FETCH f.sections s
    WHERE f.id = :id AND f.user.username = :username
""")
    Optional<Form> findFormByIdAndUsername(@Param("id") Long id,
                                           @Param("username") String username);

    @Query("""
    SELECT DISTINCT f FROM Form f
    LEFT JOIN FETCH f.sections s
    WHERE f.user.username = :username
    AND f.published = :published
""")
    List<Form> findFormsByUsernameAndStatus(@Param("username") String username,
                                            @Param("published") boolean published);
}