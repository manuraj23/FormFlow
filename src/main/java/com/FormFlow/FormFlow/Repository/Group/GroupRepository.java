package com.FormFlow.FormFlow.Repository.Group;
import com.FormFlow.FormFlow.Entity.GroupEntity.Group;
import com.FormFlow.FormFlow.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;


public interface GroupRepository extends JpaRepository<Group, UUID> {
    List<Group> findByOwner(User user);
}
