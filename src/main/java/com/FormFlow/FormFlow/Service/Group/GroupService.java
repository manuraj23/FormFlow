package com.FormFlow.FormFlow.Service.Group;

import com.FormFlow.FormFlow.DTO.Group.GroupCreateDTO;
import com.FormFlow.FormFlow.DTO.Group.GroupUser;
import com.FormFlow.FormFlow.Entity.GroupEntity.Group;
import com.FormFlow.FormFlow.Entity.User;
import com.FormFlow.FormFlow.Repository.Group.GroupRepository;
import com.FormFlow.FormFlow.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class GroupService {
    @Autowired
    public GroupRepository groupRepository;

    @Autowired
    public UserRepository userRepository;

    @Transactional
    public Group createGroup(GroupCreateDTO groupCreateDTO, String username) {
        User owner = userRepository.findByUsername(username);
        if (owner == null) {
            throw new RuntimeException("User not found");
        }
        if (groupCreateDTO.getGroupName() == null || groupCreateDTO.getGroupName().trim().isEmpty()) {
            throw new RuntimeException("Group name is required");
        }
        Group group = new Group();

        group.setGroupName(groupCreateDTO.getGroupName());
        group.setDescription(groupCreateDTO.getDescription());
        group.setPrivate(groupCreateDTO.getIsPrivate() != null ? groupCreateDTO.getIsPrivate():true);
        group.setImageUrl(groupCreateDTO.getImageUrl());
        group.setMaxMembers(groupCreateDTO.getMaxMembers()!=null ? groupCreateDTO.getMaxMembers():100);
        group.setDeleted(false);
        group.setOwner(owner);
        group.getMembers().add(owner);
        group.getAdmins().add(owner);
        group.setCreatedAt(LocalDateTime.now());
        group.setUpdatedAt(LocalDateTime.now());
        return groupRepository.save(group);
    }

    public List<Group> getGroupsOfUser(String username) {
        User user = userRepository.findByUsername(username);
        return groupRepository.findByOwner(user);
    }

    @Transactional(readOnly = true)
    public List<GroupUser> getGroupAdmins(UUID groupId) {
        Group group = getGroupOrThrow(groupId);
        return group.getAdmins().stream().map(this::mapToUserDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<GroupUser> getGroupMembers(UUID groupId) {
        Group group = getGroupOrThrow(groupId);
        return group.getMembers().stream().map(this::mapToUserDTO).toList();
    }

    private Group getGroupOrThrow(UUID groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        if (group.isDeleted()) {
            throw new RuntimeException("Group not found");
        }
        return group;
    }

    private GroupUser mapToUserDTO(User user) {
        GroupUser dto = new GroupUser();
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        return dto;
    }
}
