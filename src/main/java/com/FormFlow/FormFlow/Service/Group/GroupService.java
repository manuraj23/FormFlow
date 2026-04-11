package com.FormFlow.FormFlow.Service.Group;

import com.FormFlow.FormFlow.DTO.Group.GroupCreateDTO;
import com.FormFlow.FormFlow.DTO.Group.GroupUser;
import com.FormFlow.FormFlow.Entity.GroupEntity.Group;
import com.FormFlow.FormFlow.Entity.GroupEntity.GroupInvite;
import com.FormFlow.FormFlow.Entity.User;
import com.FormFlow.FormFlow.Repository.Group.GroupInviteRepository;
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

    @Autowired
    private GroupInviteRepository inviteRepository;

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

    @Transactional
    public void addMembers(UUID groupId, List<String> usernames, String currentUsername) {
        Group group = getGroupOrThrow(groupId);
        User currentUser = userRepository.findByUsername(currentUsername);
        if (currentUser == null) {
            throw new RuntimeException("User not found");
        }
        if (!group.getOwner().equals(currentUser) && !group.getAdmins().contains(currentUser)) {
            throw new RuntimeException("Only Owner or Admin can add members");
        }
        for (String username : usernames) {
            User user = userRepository.findByUsername(username);
            if (user == null) {
                throw new RuntimeException("User not found: " + username);
            }
            if (group.getMembers().contains(user)) {
                continue;
            }
            if (group.getMembers().size() >= group.getMaxMembers()) {
                throw new RuntimeException("Group member limit reached");
            }
            group.getMembers().add(user);
        }
        group.setUpdatedAt(LocalDateTime.now());
        groupRepository.save(group);
    }

    @Transactional
    public void addAdmins(UUID groupId, List<String> usernames, String currentUsername) {

        Group group = getGroupOrThrow(groupId);
        User currentUser = userRepository.findByUsername(currentUsername);
        if (currentUser == null) {
            throw new RuntimeException("User not found");
        }
        if (!group.getOwner().equals(currentUser)) {
            throw new RuntimeException("Only Owner can assign admins");
        }
        for (String username : usernames) {
            User user = userRepository.findByUsername(username);
            if (user == null) {
                throw new RuntimeException("User not found: " + username);
            }
            if (!group.getMembers().contains(user)) {
                if (group.getMembers().size() >= group.getMaxMembers()) {
                    throw new RuntimeException("Group member limit reached");
                }
                group.getMembers().add(user);
            }
            group.getAdmins().add(user);
        }
        group.setUpdatedAt(LocalDateTime.now());
        groupRepository.save(group);
    }

    @Transactional
    public void removeAdmins(UUID groupId, List<String> usernames, String currentUsername) {
        Group group = getGroupOrThrow(groupId);
        User currentUser = userRepository.findByUsername(currentUsername);
        if (currentUser == null) {
            throw new RuntimeException("User not found");
        }
        if (!group.getOwner().equals(currentUser)) {
            throw new RuntimeException("Only Owner can demote admins");
        }
        for (String username : usernames) {
            User user = userRepository.findByUsername(username);
            if (user == null) {
                throw new RuntimeException("User not found: " + username);
            }
            if (user.equals(group.getOwner())) {
                throw new RuntimeException("Owner cannot be demoted");
            }
            if (group.getAdmins().contains(user)) {
                group.getAdmins().remove(user);
            }
        }
        group.setUpdatedAt(LocalDateTime.now());
        groupRepository.save(group);
    }

    @Transactional
    public String generateInviteLink(UUID groupId, String currentUsername, int minutesValid) {

        Group group = getGroupOrThrow(groupId);
        User currentUser = userRepository.findByUsername(currentUsername);

        if (!group.getOwner().equals(currentUser) && !group.getAdmins().contains(currentUser)) {
            throw new RuntimeException("Only admin/owner can generate invite link");
        }
        String token = UUID.randomUUID().toString();
        GroupInvite invite = new GroupInvite();
        invite.setToken(token);
        invite.setGroup(group);
        invite.setCreatedAt(LocalDateTime.now());
        invite.setExpiryTime(LocalDateTime.now().plusMinutes(minutesValid));
        invite.setActive(true);
        inviteRepository.save(invite);
        return "http://localhost:4200/join-group?token=" + token;
    }

    @Transactional
    public void joinGroupByInvite(String token, String username) {

        GroupInvite invite = inviteRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid invite link"));

        if (!invite.isActive()) {
            throw new RuntimeException("Invite link is inactive");
        }
        if (invite.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Invite link expired");
        }
        Group group = invite.getGroup();
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        if (group.getMembers().contains(user)) {
            return;
        }
        if (group.getMembers().size() >= group.getMaxMembers()) {
            throw new RuntimeException("Group is full");
        }
        group.getMembers().add(user);
        group.setUpdatedAt(LocalDateTime.now());
        groupRepository.save(group);
    }

    @Transactional
    public void removeUsers(UUID groupId, List<String> usernames, String currentUsername) {
        Group group = getGroupOrThrow(groupId);
        User currentUser = userRepository.findByUsername(currentUsername);
        if (currentUser == null) {
            throw new RuntimeException("User not found");
        }
        if (!group.getOwner().equals(currentUser) && !group.getAdmins().contains(currentUser)) {
            throw new RuntimeException("Only Owner/Admin can remove users");
        }
        for (String username : usernames) {
            User user = userRepository.findByUsername(username);
            if (user == null) {
                throw new RuntimeException("User not found: " + username);
            }
            if (user.equals(group.getOwner())) {
                throw new RuntimeException("Owner cannot be removed");
            }
            if (group.getAdmins().contains(user) && !group.getOwner().equals(currentUser)) {
                throw new RuntimeException("Only Owner can remove an admin");
            }
            group.getAdmins().remove(user);
            group.getMembers().remove(user);
        }
        group.setUpdatedAt(LocalDateTime.now());
        groupRepository.save(group);
    }

}
