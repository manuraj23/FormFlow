package com.FormFlow.FormFlow.Service.Group;

import com.FormFlow.FormFlow.DTO.Group.GroupCreateDTO;
import com.FormFlow.FormFlow.DTO.Group.GroupResponseDTO;
import com.FormFlow.FormFlow.DTO.Group.GroupUser;
import com.FormFlow.FormFlow.Entity.Form;
import com.FormFlow.FormFlow.Entity.GroupEntity.Group;
import com.FormFlow.FormFlow.Entity.GroupEntity.GroupInvite;
import com.FormFlow.FormFlow.Entity.User;
import com.FormFlow.FormFlow.Entity.UserFormRole;
import com.FormFlow.FormFlow.Repository.FormRepository;
import com.FormFlow.FormFlow.Repository.Group.GroupInviteRepository;
import com.FormFlow.FormFlow.Repository.Group.GroupRepository;
import com.FormFlow.FormFlow.Repository.UserFormRoleRepository;
import com.FormFlow.FormFlow.Repository.UserRepository;
import com.FormFlow.FormFlow.enums.RoleType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class GroupService {
    @Autowired
    public GroupRepository groupRepository;

    @Autowired
    public UserRepository userRepository;

    @Autowired
    private GroupInviteRepository inviteRepository;

    @Autowired
    private FormRepository formRepository;

    @Autowired
    private UserFormRoleRepository userFormRoleRepository;

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

    public GroupResponseDTO createGroupResponse(Group group) {
        return mapToGroupResponseDTO(group);
    }

    public List<GroupResponseDTO> getGroupsOfUser(String username) {
        User user = userRepository.findByUsername(username);
        return groupRepository.findByOwner(user).stream().map(this::mapToGroupResponseDTO).toList();
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

    private GroupResponseDTO mapToGroupResponseDTO(Group group) {
        GroupResponseDTO dto = new GroupResponseDTO();
        dto.setGroupId(group.getGroupId());
        dto.setGroupName(group.getGroupName());
        dto.setDescription(group.getDescription());
        dto.setImageUrl(group.getImageUrl());
        dto.setMaxMembers(group.getMaxMembers());
        dto.setIsPrivate(group.isPrivate());
        dto.setCreatedAt(group.getCreatedAt());
        dto.setUpdatedAt(group.getUpdatedAt());
        if (group.getOwner() != null) {
            dto.setOwnerId(group.getOwner().getUserId());
            dto.setOwnerUsername(group.getOwner().getUsername());
            dto.setOwnerEmail(group.getOwner().getEmail());
        }
        return dto;
    }

    @Transactional
    public void addMembers(UUID groupId, List<String> emails, String currentUsername) {
        Group group = getGroupOrThrow(groupId);
        User currentUser = userRepository.findByUsername(currentUsername);
        if (currentUser == null) {
            throw new RuntimeException("User not found");
        }
        if (!isOwnerOrAdmin(group, currentUser)) {
            throw new RuntimeException("Only Owner or Admin can add members");
        }
        for (String email : emails) {
            if (email == null || email.trim().isEmpty()) {
                continue;
            }
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
            if (containsUserById(group.getMembers(), user)) {
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
    public void addAdmins(UUID groupId, List<String> userEmails, String currentUsername) {

        Group group = getGroupOrThrow(groupId);
        User currentUser = userRepository.findByUsername(currentUsername);
        if (currentUser == null) {
            throw new RuntimeException("User not found");
        }
        if (!isSameUser(group.getOwner(), currentUser)) {
            throw new RuntimeException("Only Owner can assign admins");
        }
        for (String userEmail : userEmails) {
            if (userEmail == null || userEmail.trim().isEmpty()) {
                continue;
            }
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + userEmail));
            if (!containsUserById(group.getMembers(), user)) {
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
    public void removeAdmins(UUID groupId, List<String> userEmails, String currentUsername) {
        Group group = getGroupOrThrow(groupId);
        User currentUser = userRepository.findByUsername(currentUsername);
        if (currentUser == null) {
            throw new RuntimeException("User not found");
        }
        if (!isSameUser(group.getOwner(), currentUser)) {
            throw new RuntimeException("Only Owner can demote admins");
        }
        for (String userEmail : userEmails) {
            if (userEmail == null || userEmail.trim().isEmpty()) {
                continue;
            }
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));
            if (isSameUser(user, group.getOwner())) {
                throw new RuntimeException("Owner cannot be demoted");
            }
            removeUserById(group.getAdmins(), user);
        }
        group.setUpdatedAt(LocalDateTime.now());
        groupRepository.save(group);
    }

    @Transactional
    public String generateInviteLink(UUID groupId, String currentUsername, int minutesValid) {

        Group group = getGroupOrThrow(groupId);
        User currentUser = userRepository.findByUsername(currentUsername);
        if (currentUser == null) {
            throw new RuntimeException("User not found");
        }
        if (minutesValid <= 0) {
            throw new RuntimeException("Invite validity must be greater than 0 minutes");
        }

        if (!isOwnerOrAdmin(group, currentUser)) {
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
        if (group == null || group.isDeleted()) {
            throw new RuntimeException("Group not found");
        }
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        if (containsUserById(group.getMembers(), user)) {
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
    public void removeUsers(UUID groupId, List<String> userEmails, String currentUsername) {
        Group group = getGroupOrThrow(groupId);
        User currentUser = userRepository.findByUsername(currentUsername);
        if (currentUser == null) {
            throw new RuntimeException("User not found");
        }
        if (!isOwnerOrAdmin(group, currentUser)) {
            throw new RuntimeException("Only Owner/Admin can remove users");
        }
        for (String userEmail : userEmails) {
            if (userEmail == null || userEmail.trim().isEmpty()) {
                continue;
            }
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));
            if (isSameUser(user, group.getOwner())) {
                throw new RuntimeException("Owner cannot be removed");
            }
            if (containsUserById(group.getAdmins(), user) && !isSameUser(group.getOwner(), currentUser)) {
                throw new RuntimeException("Only Owner can remove an admin");
            }
            removeUserById(group.getAdmins(), user);
            removeUserById(group.getMembers(), user);
        }
        group.setUpdatedAt(LocalDateTime.now());
        groupRepository.save(group);
    }

    public GroupResponseDTO updateGroupDetails(UUID groupId, GroupCreateDTO groupCreateDTO, String currentUsername) {
        Group group = getGroupOrThrow(groupId);
        User currentUser = userRepository.findByUsername(currentUsername);
        if (currentUser == null) {
            throw new RuntimeException("User not found");
        }
        if (!isSameUser(group.getOwner(), currentUser)) {
            throw new RuntimeException("Only Owner can update group details");
        }
        if (groupCreateDTO.getGroupName() != null && !groupCreateDTO.getGroupName().trim().isEmpty()) {
            group.setGroupName(groupCreateDTO.getGroupName());
        }
        if (groupCreateDTO.getDescription() != null) {
            group.setDescription(groupCreateDTO.getDescription());
        }
        if (groupCreateDTO.getImageUrl() != null) {
            group.setImageUrl(groupCreateDTO.getImageUrl());
        }
        if (groupCreateDTO.getIsPrivate() != null) {
            group.setPrivate(groupCreateDTO.getIsPrivate());
        }
        if (groupCreateDTO.getMaxMembers() != null) {
            if (groupCreateDTO.getMaxMembers() < group.getMembers().size()) {
                throw new RuntimeException("Max members cannot be less than current member count");
            }
            group.setMaxMembers(groupCreateDTO.getMaxMembers());
        }
        group.setUpdatedAt(LocalDateTime.now());
        Group updatedGroup = groupRepository.save(group);
        return mapToGroupResponseDTO(updatedGroup);
    }

    @Transactional
    public int assignFormToGroup(UUID groupId, UUID formId, String currentUsername,RoleType roleType) {
        Group group = getGroupOrThrow(groupId);
        User currentUser = userRepository.findByUsername(currentUsername);
        if (currentUser == null) {
            throw new RuntimeException("User not found");
        }
        if (!isOwnerOrAdmin(group, currentUser)) {
            throw new RuntimeException("Only Owner/Admin can assign forms to this group");
        }

        Form form = formRepository.findById(formId)
                .orElseThrow(() -> new RuntimeException("Form not found"));
        if (form.isDeleted()) {
            throw new RuntimeException("Form not found");
        }
        if (!isSameUser(form.getUser(), currentUser)) {
            throw new RuntimeException("Only Form Owner can assign this form to a group");
        }

        Map<UUID, User> targetUsersById = new HashMap<>();
        addUsersById(targetUsersById, group.getMembers());
        addUsersById(targetUsersById, group.getAdmins());
        addUsersById(targetUsersById, Set.of(group.getOwner()));

        List<UserFormRole> existingRoles = userFormRoleRepository.findByFormId(formId);
        Set<UUID> existingUserIds = new HashSet<>();
        for (UserFormRole role : existingRoles) {
            if (role.getUser() != null) {
                existingUserIds.add(role.getUser().getUserId());
            }
        }

        List<UserFormRole> toSave = new ArrayList<>();
        for (User user : targetUsersById.values()) {
            if (isSameUser(user, form.getUser())) {
                continue;
            }
            if (existingUserIds.contains(user.getUserId())) {
                continue;
            }

            UserFormRole role = new UserFormRole();
            role.setUser(user);
            role.setForm(form);
            role.setRole(roleType != null ? roleType : RoleType.VIEWER);
            role.setViewed(false);
            role.setAssignedAt(LocalDateTime.now());
            role.setAssignedBy(currentUsername);
            toSave.add(role);
        }

        if (!toSave.isEmpty()) {
            userFormRoleRepository.saveAll(toSave);
        }

        return toSave.size();
    }

    @Transactional
    public int updateGroupRole(UUID groupId, UUID formId, String currentUsername, RoleType newRole) {

        Group group = getGroupOrThrow(groupId);
        User currentUser = userRepository.findByUsername(currentUsername);
        if (currentUser == null) {
            throw new RuntimeException("User not found");
        }
        if (!isOwnerOrAdmin(group, currentUser)) {
            throw new RuntimeException("Only Owner/Admin can update roles");
        }
        Form form = formRepository.findById(formId)
                .orElseThrow(() -> new RuntimeException("Form not found"));

        if (form.isDeleted()) {
            throw new RuntimeException("Form not found");
        }
        if (!isSameUser(form.getUser(), currentUser)) {
            throw new RuntimeException("Only Form Owner can update roles");
        }
        Map<UUID, User> targetUsers = new HashMap<>();
        addUsersById(targetUsers, group.getMembers());
        addUsersById(targetUsers, group.getAdmins());
        addUsersById(targetUsers, Set.of(group.getOwner()));
        List<UserFormRole> roles = userFormRoleRepository.findByFormId(formId);
        int updatedCount = 0;
        for (UserFormRole ufr : roles) {
            User user = ufr.getUser();
            if (user == null) continue;
            if (!targetUsers.containsKey(user.getUserId())) continue;
            if (isSameUser(user, form.getUser())) continue;
            ufr.setRole(newRole);
            updatedCount++;
        }
        userFormRoleRepository.saveAll(roles);
        return updatedCount;
    }

    private boolean isOwnerOrAdmin(Group group, User user) {
        return isSameUser(group.getOwner(), user) || containsUserById(group.getAdmins(), user);
    }

    private void addUsersById(Map<UUID, User> usersById, Collection<User> users) {
        for (User user : users) {
            if (user != null && user.getUserId() != null) {
                usersById.putIfAbsent(user.getUserId(), user);
            }
        }
    }

    private boolean containsUserById(Collection<User> users, User target) {
        if (target == null || target.getUserId() == null) {
            return false;
        }
        UUID targetId = target.getUserId();
        return users.stream().anyMatch(user -> user != null && targetId.equals(user.getUserId()));
    }

    private void removeUserById(Set<User> users, User target) {
        if (target == null || target.getUserId() == null) {
            return;
        }
        UUID targetId = target.getUserId();
        users.removeIf(user -> user != null && targetId.equals(user.getUserId()));
    }

    private boolean isSameUser(User userA, User userB) {
        if (userA == null || userB == null || userA.getUserId() == null || userB.getUserId() == null) {
            return false;
        }
        return userA.getUserId().equals(userB.getUserId());
    }
}
