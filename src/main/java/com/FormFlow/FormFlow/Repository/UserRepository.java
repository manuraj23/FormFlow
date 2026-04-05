package com.FormFlow.FormFlow.Repository;

import com.FormFlow.FormFlow.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    User findByUsername(String username);
    boolean existsByUsername(String username);
    void deleteByUsername(String username);
}