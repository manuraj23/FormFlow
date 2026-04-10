package com.FormFlow.FormFlow.Service.User;

import com.FormFlow.FormFlow.DTO.User.FormAccessDTO;
import com.FormFlow.FormFlow.DTO.User.FormAccessShareDTO;
import com.FormFlow.FormFlow.Entity.Form;
import com.FormFlow.FormFlow.Entity.User;
import com.FormFlow.FormFlow.Entity.UserFormRole;
import com.FormFlow.FormFlow.enums.RoleType;
import com.FormFlow.FormFlow.Repository.UserFormRoleRepository;
import com.FormFlow.FormFlow.Repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

        // Fetch existing roles
        List<UserFormRole> existingRoles = roleRepo.findByFormId(formId);

        Map<UUID, UserFormRole> existingMap = existingRoles.stream()
                .collect(Collectors.toMap(r -> r.getUser().getUserId(), r -> r));

        Map<UUID, RoleType> newRolesMap = new HashMap<>();

//        // OWNER
//        User owner = userRepo.findByUsername(dto.getOwner());
//        if (owner == null) throw new RuntimeException("Owner not found");
//        newRolesMap.put(owner.getUserId(), RoleType.OWNER);

        // EDITOR
        if (dto.getAccess().getEditor() != null) {
            dto.getAccess().getEditor().forEach(username -> {
                User user = getUser(username);
                newRolesMap.put(user.getUserId(), RoleType.EDITOR);
            });
        }

        // RESPONDER
        if (dto.getAccess().getResponder() != null) {
            dto.getAccess().getResponder().forEach(username -> {
                User user = getUser(username);
                newRolesMap.put(user.getUserId(), RoleType.RESPONDER);
            });
        }

        // VIEWER
        if (dto.getAccess().getViewer() != null) {
            dto.getAccess().getViewer().forEach(username -> {
                User user = getUser(username);
                newRolesMap.put(user.getUserId(), RoleType.VIEWER);
            });
        }

        List<UserFormRole> toDelete = new ArrayList<>();
        List<UserFormRole> newEntities = new ArrayList<>();

        // HANDLE EXISTING USERS
        for (UserFormRole existing : existingRoles) {

            UUID userId = existing.getUser().getUserId();

            if (newRolesMap.containsKey(userId)) {

                RoleType newRole = newRolesMap.get(userId);

                // Update role only (DO NOT TOUCH MESSAGE)
                if (existing.getRole() != newRole) {
                    existing.setRole(newRole);
                }

                newRolesMap.remove(userId);

            } else {
                // User removed
                toDelete.add(existing);
            }
        }

        // HANDLE NEW USERS
        for (Map.Entry<UUID, RoleType> entry : newRolesMap.entrySet()) {

            UserFormRole newRole = new UserFormRole();

            User user = userRepo.findById(entry.getKey())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Form form = new Form();
            form.setId(formId);

            newRole.setUser(user);
            newRole.setForm(form);
            newRole.setRole(entry.getValue());
            newRole.setViewed(false);
            newRole.setAssignedAt(LocalDateTime.now());
            
            //  JUST STORE MESSAGE
            String message = null;
            if (dto.getAccess().getMessage() != null && !dto.getAccess().getMessage().isEmpty()) {
                message = dto.getAccess().getMessage().get(0);
            }
            newRole.setMessage(message);
            newEntities.add(newRole);
        }

        // DELETE removed users
        if (!toDelete.isEmpty()) {
            roleRepo.deleteAll(toDelete);
        }

        // SAVE new users
        if (!newEntities.isEmpty()) {
            roleRepo.saveAll(newEntities);
        }
    }

    // GET SHARED FORMS
    public Map<String, List<FormAccessShareDTO>> getSharedForms(UUID userId) {

        List<UserFormRole> newForms = roleRepo.findByUser_UserIdAndIsViewedFalseAndForm_IsDeletedFalse(userId);
        List<UserFormRole> viewedForms = roleRepo.findByUser_UserIdAndIsViewedTrueAndForm_IsDeletedFalse(userId);
//        List<UserFormRole> newForms = roleRepo.findByUser_UserIdAndIsViewedFalse(userId);
//        List<UserFormRole> viewedForms = roleRepo.findByUser_UserIdAndIsViewedTrue(userId);

        List<FormAccessShareDTO> newDTO = newForms.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        List<FormAccessShareDTO> viewedDTO = viewedForms.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        // mark new as viewed
        newForms.forEach(f -> f.setViewed(true));
        roleRepo.saveAll(newForms);

        Map<String, List<FormAccessShareDTO>> response = new HashMap<>();
        response.put("newForms", newDTO);
        response.put("otherForms", viewedDTO);

        return response;
    }

    private FormAccessShareDTO mapToDTO(UserFormRole role) {
        FormAccessShareDTO dto = new FormAccessShareDTO();

        dto.setFormId(role.getForm().getId());
        dto.setFormName(role.getForm().getTitle());
        dto.setRole(role.getRole());
        dto.setAssignedAt(role.getAssignedAt());
        dto.setMessage(role.getMessage());
        dto.setViewed(role.isViewed());

        return dto;
    }

    // HELPERS
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

    // GET ACCESS
    public FormAccessDTO getAccess(UUID formId) {

        List<UserFormRole> roles = roleRepo.findByFormId(formId);

        FormAccessDTO dto = new FormAccessDTO();
        dto.setFormId(formId);

        FormAccessDTO.Access access = new FormAccessDTO.Access();

        List<String> editors = new ArrayList<>();
        List<String> responders = new ArrayList<>();
        List<String> viewers = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        for (UserFormRole role : roles) {

            User user = role.getUser();
            if (user == null) continue;

            if (role.getMessage() != null) {
                messages.add(role.getMessage());
            }

            switch (role.getRole()) {
//                case OWNER -> dto.setOwner(user.getUsername());
                case EDITOR -> editors.add(user.getUsername());
                case RESPONDER -> responders.add(user.getUsername());
                case VIEWER -> viewers.add(user.getUsername());
            }
        }

        access.setEditor(editors);
        access.setResponder(responders);
        access.setViewer(viewers);
        dto.setAccess(access);
        access.setMessage(messages);
        return dto;
    }

    public String getUsernameByEmail(String email) {

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        return user.getUsername();
    }
}