package com.FormFlow.FormFlow.Repository;

import com.FormFlow.FormFlow.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
    boolean existsByUsername(String username);
    void deleteByUsername(String username);
}