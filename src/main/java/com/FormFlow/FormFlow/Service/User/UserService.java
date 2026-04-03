package com.FormFlow.FormFlow.Service.User;

import com.FormFlow.FormFlow.DTO.FormDetails.FieldDTO;
import com.FormFlow.FormFlow.DTO.FormDetails.FormCreateDTO;
import com.FormFlow.FormFlow.DTO.FormDetails.FormGetDTO;
import com.FormFlow.FormFlow.DTO.FormDetails.SectionDTO;
import com.FormFlow.FormFlow.Entity.*;
import com.FormFlow.FormFlow.Repository.FormRepository;
import com.FormFlow.FormFlow.Repository.FormSectionRepository;
import com.FormFlow.FormFlow.Repository.UserRepository;
import com.FormFlow.FormFlow.enums.FieldType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.FormFlow.FormFlow.Repository.FormResponseRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    public UserRepository userRepository;

    @Autowired
    public FormRepository formRepository;

    @Autowired
    public FormSectionRepository formSectionRepository;

    @Autowired
    private FormResponseRepository formResponseRepository;

    public void createForm(FormCreateDTO dto, String username) {

        User user = userRepository.findByUsername(username);
        Form form = new Form();
        form.setTitle(dto.getTitle());
        form.setDescription(dto.getDescription());
        form.setPublished(dto.isPublished());
        form.setDeleted(false);
        form.setUser(user);

        if (dto.getSections() != null) {
            form.setSections(dto.getSections().stream().map(sectionDTO -> {

                FormSection section = new FormSection();
                section.setSectionTitle(sectionDTO.getSectionTitle());
                section.setSectionOrder(sectionDTO.getSectionOrder());
                section.setForm(form);

                if (sectionDTO.getFields() != null) {
                    section.setFields(sectionDTO.getFields().stream().map(fieldDTO -> {

                        FormFields field = new FormFields();

                        try {
                            field.setFieldType(
                                    FieldType.valueOf(fieldDTO.getFieldType().toUpperCase())
                            );
                        } catch (Exception e) {
                            throw new RuntimeException("Invalid field type: " + fieldDTO.getFieldType());
                        }

                        field.setFieldOrder(fieldDTO.getFieldOrder());
                        field.setFieldConfig(fieldDTO.getFieldConfig());
                        field.setSection(section);

                        return field;
                    }).toList());
                }

                return section;
            }).toList());
        }

        formRepository.save(form);
    }

    @Transactional(readOnly = true)
    public List<FormGetDTO> getAllForms(String username) {
        List<Form>forms = formRepository.findFormsByUsernameWithSections(username);

        forms = forms.stream().filter(form -> !form.isDeleted()).toList();
        
        if (forms.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> formIds = forms.stream().map(Form::getId).toList();
        formSectionRepository.findByFormIdInWithFields(formIds);

        return forms.stream().map(this::convertToDTO).toList();
    }
    @Transactional
    public boolean updateForm(UUID formId, FormCreateDTO dto, String username) {

        Form form = formRepository.findById(formId)
                .orElseThrow(() -> new RuntimeException("Form not found with id: " + formId));

        if (!form.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized to update this form");
        }

        List<FormResponse> responses = formResponseRepository.findByFormId(formId);
        if (responses != null && !responses.isEmpty()) {
            return false; // Version control required
        }

        if (dto.getSections() == null || dto.getSections().isEmpty()) {
            throw new RuntimeException("Form must contain at least one section");
        }

        form.setTitle(dto.getTitle() != null ? dto.getTitle() : form.getTitle());
        form.setDescription(dto.getDescription() != null ? dto.getDescription() : form.getDescription());
        form.setPublished(dto.isPublished());

        if (form.getSections() != null) {
            form.getSections().clear(); // Hibernate tracks these as orphans now
        } else {
            form.setSections(new ArrayList<>());
        }

        for (SectionDTO sectionDTO : dto.getSections()) {
            if (sectionDTO.getFields() == null || sectionDTO.getFields().isEmpty()) {
                throw new RuntimeException("Section '" + sectionDTO.getSectionTitle() + "' must have at least one field");
            }

            FormSection section = new FormSection();
            section.setSectionTitle(sectionDTO.getSectionTitle());
            section.setSectionOrder(sectionDTO.getSectionOrder());
            section.setForm(form);

            List<FormFields> fields = new ArrayList<>();
            for (FieldDTO fieldDTO : sectionDTO.getFields()) {
                FormFields field = new FormFields();
                try {
                    field.setFieldType(FieldType.valueOf(fieldDTO.getFieldType().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException(
                            "Invalid field type '" + fieldDTO.getFieldType() +
                                    "' in section '" + sectionDTO.getSectionTitle() + "'"
                    );
                }
                field.setFieldOrder(fieldDTO.getFieldOrder());
                field.setFieldConfig(fieldDTO.getFieldConfig() != null ? fieldDTO.getFieldConfig() : Collections.emptyMap());
                field.setSection(section);
                fields.add(field);
            }

            section.setFields(fields);

            form.getSections().add(section); // Add to existing mutable list
        }

        try {
            formRepository.saveAndFlush(form);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to save form: " + e.getMessage(), e);
        }

        return true;
    }
    @Transactional(readOnly = true)
    public FormGetDTO getFormById(String username, UUID id) {

        Form form = formRepository.findFormByIdAndUsername(id, username)
                .orElseThrow(() -> new RuntimeException("Form not found or not authorized"));

        if(form.isDeleted()) {
            throw new RuntimeException("Form not found or not authorized");
        }

        formSectionRepository.findByFormIdInWithFields(List.of(form.getId()));

        return convertToDTO(form);
    }


    private FormGetDTO convertToDTO(Form form) {
        FormGetDTO dto = new FormGetDTO();
        dto.setId(form.getId());
        dto.setTitle(form.getTitle());
        dto.setDescription(form.getDescription());
        dto.setPublished(form.isPublished());
        dto.setCreatedAt(form.getCreatedAt());
        dto.setCreatedBy(form.getUser().getUsername());

        if (form.getSections() != null) {
            dto.setSections(form.getSections().stream().map(section -> {
                SectionDTO sectionDTO = new SectionDTO();
                sectionDTO.setSectionTitle(section.getSectionTitle());
                sectionDTO.setSectionOrder(section.getSectionOrder());

                if (section.getFields() != null) {
                    sectionDTO.setFields(section.getFields().stream().map(field -> {
                        FieldDTO fieldDTO = new FieldDTO();
                        fieldDTO.setFieldType(field.getFieldType().name());
                        fieldDTO.setFieldOrder(field.getFieldOrder());
                        fieldDTO.setFieldConfig(field.getFieldConfig());
                        return fieldDTO;
                    }).toList());
                }

                return sectionDTO;
            }).toList());
        }

        return dto;
    }

    @Transactional(readOnly = true)
    public List<FormGetDTO> getFormsByStatus(String username, String status) {
        boolean published;
        if (status.equalsIgnoreCase("published")) {
            published = true;
        } else if (status.equalsIgnoreCase("draft")) {
            published = false;
        } else {
            throw new RuntimeException("Status must be 'published' or 'draft'");
        }
        List<Form> forms = formRepository.findFormsByUsernameAndStatus(username, published);
        forms = forms.stream().filter(form -> !form.isDeleted()).toList();

        if (forms.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> formIds = forms.stream().map(Form::getId).toList();
        formSectionRepository.findByFormIdInWithFields(formIds);

        return forms.stream()
                .map(this::convertToDTO)
                .toList();
    }

    public void softDeleteForm(String username, UUID id) {
        Form form = formRepository.findFormByIdAndUsername(id, username)
                .orElseThrow(() -> new RuntimeException("Form not found or not authorized"));
        if (form.isDeleted()) {
            return;
        }
        form.setDeleted(true);
        formRepository.save(form);
    }

    public List<FormGetDTO> getTrashedForms(String username) {
        List<Form>forms = formRepository.findFormsByUsernameWithSections(username);

        forms = forms.stream().filter(Form::isDeleted).toList();

        if (forms.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> formIds = forms.stream().map(Form::getId).toList();
        formSectionRepository.findByFormIdInWithFields(formIds);

        return forms.stream()
                .map(this::convertToDTO)
                .toList();
    }

    public void restoreDeletedForm(String username, UUID id) {
        Form form = formRepository.findFormByIdAndUsername(id, username)
                .orElseThrow(() -> new RuntimeException("Form not found or not authorized"));
        if (!form.isDeleted()) {
            return;
        }
        form.setDeleted(false);
        formRepository.save(form);
    }

}