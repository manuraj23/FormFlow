package com.FormFlow.FormFlow.Repository.User;

import com.FormFlow.FormFlow.Entity.UserEntity.TempUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

public interface TempUserRepository extends JpaRepository<TempUser, UUID> {
    Optional<TempUser> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    @Modifying
    @Transactional
    @Query("DELETE FROM TempUser t WHERE t.email = :email")
    void deleteByEmail(String email);
}
