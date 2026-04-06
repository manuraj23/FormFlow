package com.FormFlow.FormFlow.Service.User;

import com.FormFlow.FormFlow.DTO.User.FormAccessDTO;
import com.FormFlow.FormFlow.Entity.User;
import com.FormFlow.FormFlow.Entity.UserFormRole;
import com.FormFlow.FormFlow.enums.RoleType;
import com.FormFlow.FormFlow.Repository.UserFormRoleRepository;
import com.FormFlow.FormFlow.Repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FormAccessService {

    private final UserFormRoleRepository roleRepo;
    private final UserRepository userRepo;

    public FormAccessService(UserFormRoleRepository roleRepo, UserRepository userRepo) {
        this.roleRepo = roleRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    public void saveAccess(FormAccessDTO dto) {

        UUID formId = dto.getFormId();

        //Fetch existing roles
        List<UserFormRole> existingRoles = roleRepo.findByFormId(formId);

        //Map existing users
        Map<UUID, UserFormRole> existingMap = existingRoles.stream()
                .collect(Collectors.toMap(UserFormRole::getUserId, r -> r));

        //Prepare new roles map
        Map<UUID, RoleType> newRolesMap = new HashMap<>();

        // OWNER
        User owner = userRepo.findByUsername(dto.getOwner());
        if (owner == null) {
            throw new RuntimeException("Owner not found");
        }
        newRolesMap.put(owner.getUserId(), RoleType.OWNER);

        // EDITORS
        if (dto.getAccess().getEditor() != null) {
            dto.getAccess().getEditor().forEach(username -> {
                User user = getUser(username);
                newRolesMap.put(user.getUserId(), RoleType.EDITOR);
            });
        }

        // RESPONDERS
        if (dto.getAccess().getResponder() != null) {
            dto.getAccess().getResponder().forEach(username -> {
                User user = getUser(username);
                newRolesMap.put(user.getUserId(), RoleType.RESPONDER);
            });
        }

        // VIEWERS
        if (dto.getAccess().getViewer() != null) {
            dto.getAccess().getViewer().forEach(username -> {
                User user = getUser(username);
                newRolesMap.put(user.getUserId(), RoleType.VIEWER);
            });
        }

        // RESPONSE VIEWERS
        if (dto.getAccess().getResponseViewer() != null) {
            dto.getAccess().getResponseViewer().forEach(username -> {
                User user = getUser(username);
                newRolesMap.put(user.getUserId(), RoleType.RESPONSE_VIEWER);
            });
        }

        List<UserFormRole> toDelete = new ArrayList<>();
        List<UserFormRole> newEntities = new ArrayList<>();

        //Handle existing users
        for (UserFormRole existing : existingRoles) {

            UUID userId = existing.getUserId();

            if (newRolesMap.containsKey(userId)) {

                RoleType newRole = newRolesMap.get(userId);

                //Only update role if changed
                if (existing.getRole() != newRole) {
                    existing.setRole(newRole);
                }

                //DO NOT TOUCH isViewed
                // Remove processed
                newRolesMap.remove(userId);

            } else {
                // User removed
                toDelete.add(existing);
            }
        }

        // Add new users
        for (Map.Entry<UUID, RoleType> entry : newRolesMap.entrySet()) {
            UserFormRole newRole = new UserFormRole();
            newRole.setUserId(entry.getKey());
            newRole.setFormId(formId);
            newRole.setRole(entry.getValue());
            newRole.setViewed(false); //only new users

            newEntities.add(newRole);
        }

        //Apply DB changes
        if (!toDelete.isEmpty()) {
            roleRepo.deleteAll(toDelete);
        }

        if (!newEntities.isEmpty()) {
            roleRepo.saveAll(newEntities);
        }
    }

    //GET SHARED FORMS
    public Map<String, List<UserFormRole>> getSharedForms(UUID userId) {

        List<UserFormRole> newForms = roleRepo.findByUserIdAndIsViewedFalse(userId);
        List<UserFormRole> viewedForms = roleRepo.findByUserIdAndIsViewedTrue(userId);

        // mark new as viewed
        newForms.forEach(f -> f.setViewed(true));
        roleRepo.saveAll(newForms);

        Map<String, List<UserFormRole>> response = new HashMap<>();
        response.put("newForms", newForms);
        response.put("otherForms", viewedForms);

        return response;
    }

    private User getUser(String username) {
        User user = userRepo.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found: " + username);
        }
        return user;
    }

    public UUID getUserIdByUsername(String username) {
        User user = userRepo.findByUsername(username);
        if (user == null) throw new RuntimeException("User not found: " + username);
        return user.getUserId();
    }


    public FormAccessDTO getAccess(UUID formId) {

        List<UserFormRole> roles = roleRepo.findByFormId(formId);

        FormAccessDTO dto = new FormAccessDTO();
        dto.setFormId(formId);

        FormAccessDTO.Access access = new FormAccessDTO.Access();

        List<String> editors = new ArrayList<>();
        List<String> responders = new ArrayList<>();
        List<String> viewers = new ArrayList<>();
        List<String> responseViewers = new ArrayList<>();

        for (UserFormRole role : roles) {

            User user = userRepo.findById(role.getUserId())
                    .orElse(null);

            if (user == null) continue;

            switch (role.getRole()) {
                case OWNER -> dto.setOwner(user.getUsername());
                case EDITOR -> editors.add(user.getUsername());
                case RESPONDER -> responders.add(user.getUsername());
                case VIEWER -> viewers.add(user.getUsername());
                case RESPONSE_VIEWER -> responseViewers.add(user.getUsername());

            }
        }

        access.setEditor(editors);
        access.setResponder(responders);
        access.setViewer(viewers);
        access.setResponseViewer(responseViewers);

        dto.setAccess(access);

        return dto;
    }
}