package com.FormFlow.FormFlow.Repository;

import com.FormFlow.FormFlow.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}