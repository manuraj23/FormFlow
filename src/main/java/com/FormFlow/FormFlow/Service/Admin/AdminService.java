package com.FormFlow.FormFlow.Service.Admin;

import com.FormFlow.FormFlow.DTO.FormDetails.FieldDTO;
import com.FormFlow.FormFlow.DTO.FormDetails.FormGetDTO;
import com.FormFlow.FormFlow.DTO.FormDetails.SectionDTO;
import com.FormFlow.FormFlow.DTO.User.UserDTO;
import com.FormFlow.FormFlow.Entity.Form;
import com.FormFlow.FormFlow.Repository.FormSectionRepository;
import com.FormFlow.FormFlow.Entity.User;
import com.FormFlow.FormFlow.Repository.FormRepository;
import com.FormFlow.FormFlow.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class AdminService {

    @Autowired
    private FormRepository formRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FormSectionRepository formSectionRepository;

    public List<UserDTO> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream().map(user -> {
            UserDTO dto = new UserDTO();
            dto.setUserId(user.getUserId());
            dto.setUsername(user.getUsername());
            dto.setEmail(user.getEmail());
            dto.setRoles(user.getRoles());
            return dto;
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<FormGetDTO> getAllForms() {
        List<Form> forms = formRepository.findAllFormsWithSections();
        if (forms.isEmpty()) {
            return Collections.emptyList();
        }
        List<UUID> formIds = forms.stream().map(Form::getId).toList();
        formSectionRepository.findByFormIdInWithFields(formIds);
        return forms.stream()
                .map(this::convertToDTO)
                .toList();
    }

    private FormGetDTO convertToDTO(Form form) {
        FormGetDTO dto = new FormGetDTO();
        dto.setId(form.getId());
        dto.setTheme(form.getTheme());
        dto.setTitle(form.getTitle());
        dto.setDescription(form.getDescription());
        dto.setPublished(form.isPublished());
        dto.setSettings(form.getSettings());
        dto.setCreatedAt(form.getCreatedAt());
        dto.setCreatedBy(form.getUser().getUsername());

        if (form.getSections() != null) {
            dto.setSections(form.getSections().stream().map(section -> {
                SectionDTO sectionDTO = new SectionDTO();
                sectionDTO.setId(section.getId());
                sectionDTO.setSectionTitle(section.getSectionTitle());
                sectionDTO.setSectionOrder(section.getSectionOrder());

                if (section.getFields() != null) {
                    sectionDTO.setFields(section.getFields().stream().map(field -> {
                        FieldDTO fieldDTO = new FieldDTO();
                        fieldDTO.setId(field.getId());
                        fieldDTO.setFieldType(field.getFieldType().name());
                        fieldDTO.setFieldOrder(field.getFieldOrder());
                        fieldDTO.setFieldStyle(field.getFieldStyle());
                        fieldDTO.setFieldConfig(field.getFieldConfig());
                        return fieldDTO;
                    }).toList());
                }
                return sectionDTO;
            }).toList());
        }
        return dto;
    }
}
