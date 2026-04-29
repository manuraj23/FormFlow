package com.FormFlow.FormFlow.Service.User;

import com.FormFlow.FormFlow.DTO.FormDetails.FieldDTO;
import com.FormFlow.FormFlow.DTO.FormDetails.FormCreateDTO;
import com.FormFlow.FormFlow.DTO.FormDetails.FormGetDTO;
import com.FormFlow.FormFlow.DTO.FormDetails.SectionDTO;
import com.FormFlow.FormFlow.DTO.FormDetails.Version.VersionResponseDTO;
import com.FormFlow.FormFlow.DTO.UpdateFormDTO;
import com.FormFlow.FormFlow.Entity.*;
import com.FormFlow.FormFlow.Repository.*;
import com.FormFlow.FormFlow.enums.FieldType;
import com.FormFlow.FormFlow.enums.RoleType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

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

    @Autowired
    private UserFormRoleRepository userFormRoleRepository;


    @Transactional
    public UUID createForm(FormCreateDTO dto, String username) {

        User user = userRepository.findByUsername(username);
        Form form = new Form();
        form.setTheme(dto.getTheme());
        form.setTitle(dto.getTitle());
        form.setDescription(dto.getDescription());
        form.setPublished(dto.isPublished());
        form.setEditable(!dto.isPublished());
        form.setSettings(dto.getSettings());
        form.setDeleted(false);
        form.setUser(user);
        // mapping for old field IDs → new field IDs (used in versioning fix)
        Map<String, String> oldToNewIdMap = new HashMap<>();
        if (dto.getMainParentId() != null) {
            form.setMainParentId(dto.getMainParentId());
            Integer maxVersion = formRepository.findMaxVersionByParentId(dto.getMainParentId());
            form.setVersionId((maxVersion == null) ? 1 : maxVersion + 1);
            if (form.isPublished()) {
                UUID parentId = dto.getMainParentId();
                formRepository.deactivateAllVersions(parentId);
            }

        } else {
            form.setVersionId(1);
        }
        if (dto.getSections() != null) {
            List<FormSection> sections = new ArrayList<>();
            for (SectionDTO sectionDTO : dto.getSections()) {

                FormSection section = new FormSection();
                section.setSectionTitle(sectionDTO.getSectionTitle());
                section.setSectionOrder(sectionDTO.getSectionOrder());
                section.setForm(form);

                if (sectionDTO.getFields() != null) {
                    List<FormFields> fields = new ArrayList<>();
                    for (FieldDTO fieldDTO : sectionDTO.getFields()) {
                        FormFields field = new FormFields();
                        // field type
                        try {
                            field.setFieldType(
                                    FieldType.valueOf(fieldDTO.getFieldType().toUpperCase())
                            );
                        } catch (Exception e) {
                            throw new RuntimeException(
                                    "Invalid field type: " + fieldDTO.getFieldType()
                            );
                        }

                        if (dto.getMainParentId() == null) {
                            if (fieldDTO.getId() != null && !fieldDTO.getId().isBlank()) {
                                field.setId(UUID.fromString(fieldDTO.getId()));
                            }
                        } else {
                            UUID newId = UUID.randomUUID();
                            field.setId(newId);
                            if (fieldDTO.getId() != null && !fieldDTO.getId().isBlank()) {
                                oldToNewIdMap.put(fieldDTO.getId(), newId.toString());
                            }
                        }

                        field.setFieldOrder(fieldDTO.getFieldOrder());
                        field.setFieldConfig(fieldDTO.getFieldConfig());
                        field.setFieldStyle(fieldDTO.getFieldStyle());
                        field.setQuizConfig(fieldDTO.getQuizConfig());
                        field.setFieldLogic(fieldDTO.getFieldLogic());

                        field.setSection(section);
                        fields.add(field);
                    }
                    section.setFields(fields);
                }
                sections.add(section);
            }
            form.setSections(sections);
        }

        if (dto.getMainParentId() != null && !oldToNewIdMap.isEmpty()) {
            form.getSections().forEach(section -> section.getFields().forEach(field -> {
                Map<String, Object> logic = field.getFieldLogic();

                if (logic != null && Boolean.TRUE.equals(logic.get("enabled")) && logic.get("sourceFieldId") != null){
                            String oldSourceId = (String) logic.get("sourceFieldId");

                            if (oldToNewIdMap.containsKey(oldSourceId)) {
                                Map<String, Object> updatedLogic = new HashMap<>(logic);
                                updatedLogic.put("sourceFieldId", oldToNewIdMap.get(oldSourceId));
                                field.setFieldLogic(updatedLogic);
                            }
                        }
                    })
            );
        }

        formRepository.save(form);

        // set mainParentId for first version
        if (form.getVersionId() == 1){
            form.setMainParentId(form.getId());
            formRepository.save(form);
        }

        return form.getId();
    }


    @Transactional(readOnly = true)
    public List<FormGetDTO> getAllForms(String username) {

        List<Form> forms = formRepository.findFormsByUsernameWithSections(username)
                .stream()
                .filter(form -> !form.isDeleted())
                .collect(Collectors.toList());

        if (forms.isEmpty()) {
            return Collections.emptyList();
        }

        // GROUP BY parentId safely
        Map<UUID, List<Form>> groupedForms = forms.stream()
                .collect(Collectors.groupingBy(
                        form -> form.getMainParentId() != null
                                ? form.getMainParentId()
                                : form.getId()
                ));

        List<Form> finalForms = new ArrayList<>();

        for (List<Form> versionList : groupedForms.values()) {

            //  PRIORITY 1 → ACTIVE FORM
            Optional<Form> activeForm = versionList.stream()
                    .filter(Form::isPublished)
                    .max(Comparator.comparingInt(Form::getVersionId)); // latest published

            if (activeForm.isPresent()) {
                finalForms.add(activeForm.get());
                continue;
            }

            //  PRIORITY 2 → LATEST VERSION
            versionList.stream()
                    .max(Comparator.comparingInt(Form::getVersionId))
                    .ifPresent(finalForms::add);
        }

        // Load sections
        List<UUID> formIds = finalForms.stream()
                .map(Form::getId)
                .collect(Collectors.toList());

        formSectionRepository.findByFormIdInWithFields(formIds);

        return finalForms.stream()
                .map(form -> {
                    FormGetDTO dto = convertToDTO(form);

                    //  including total responses for this end point
                    long responseCount = formResponseRepository.countByForm_Id(form.getId());
                    dto.setTotalResponses(responseCount);

                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FormGetDTO getFormById(String username, UUID id) {

        Form form = formRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Form not found"));
        boolean isEditor = userFormRoleRepository
                .findByUser_UsernameAndForm_Id(username, form.getId())
                .map(role -> role.getRole() == RoleType.EDITOR)
                .orElse(false);

        boolean isViewer = userFormRoleRepository
                .findByUser_UsernameAndForm_Id(username, form.getId())
                .map(role -> role.getRole() == RoleType.VIEWER)
                .orElse(false);

        boolean isOwner = form.getUser().getUsername().equals(username);
        if (!(isEditor || isOwner || isViewer)) {
            throw new RuntimeException("Unauthorized to update this form");
        }
        if(form.isDeleted()) {
            throw new RuntimeException("Form not found or not authorized");
        }

        formSectionRepository.findByFormIdInWithFields(List.of(form.getId()));

        return convertToDTO(form);
    }

    @Transactional
    public boolean updateForm(UUID formId, UpdateFormDTO dto, String username) {

        Form form = formRepository.findById(formId)
                .orElseThrow(() -> new RuntimeException("Form not found with id: " + formId));

        boolean isEditor = userFormRoleRepository
                .findByUser_UsernameAndForm_Id(username, form.getId())
                .map(role -> role.getRole() == RoleType.EDITOR)
                .orElse(false);

        boolean isOwner = form.getUser().getUsername().equals(username);
        if (!(isEditor || isOwner)) {
            throw new RuntimeException("Unauthorized to update this form");
        }
        List<FormResponse> responses = formResponseRepository.findByForm_Id(formId);
        if (responses != null && !responses.isEmpty()) {
            return false; // force versioning
        }
        if(!form.isEditable()){
            throw new RuntimeException("Can not edit form once published");
        }

        if (dto.getTitle() != null) form.setTitle(dto.getTitle());
        if (dto.getTheme() != null) form.setTheme(dto.getTheme());
        if (dto.getDescription() != null) form.setDescription(dto.getDescription());

        if (dto.isPublished()) {
            UUID parentId = form.getMainParentId() != null ? form.getMainParentId() : form.getId();
            formRepository.deactivateOtherVersions(parentId, form.getId());
            form.setPublished(true);
            form.setEditable(false);
            deleteEditor(formId);
        } else {
            form.setPublished(false);
        }
        if (dto.getSettings() != null) {
            form.setSettings(dto.getSettings());

            Object deadlineObj = dto.getSettings().get("deadline");
            if (deadlineObj != null) {
                try {
                    Instant.parse(deadlineObj.toString());
                } catch (Exception e) {
                    throw new RuntimeException("Invalid deadline format. Expected UTC ISO-8601");
                }
            }
        }

        if (dto.getSections() == null || dto.getSections().isEmpty()) {
            throw new RuntimeException("Form must contain at least one section");
        }

        if (form.getSections() != null) {
            form.getSections().clear();
        } else {
            form.setSections(new ArrayList<>());
        }

        for (SectionDTO sectionDTO : dto.getSections()) {

            if (sectionDTO.getFields() == null || sectionDTO.getFields().isEmpty()) {
                throw new RuntimeException(
                        "Section '" + sectionDTO.getSectionTitle() + "' must have at least one field"
                );
            }

            FormSection section = new FormSection();
            // preserve existing section ID if frontend sends it
            if (sectionDTO.getId() != null && !sectionDTO.getId().isBlank()) {
                section.setId(UUID.fromString(sectionDTO.getId()));
            }
            section.setSectionTitle(sectionDTO.getSectionTitle());
            section.setSectionOrder(sectionDTO.getSectionOrder());
            section.setForm(form);

            List<FormFields> fields = new ArrayList<>();

            for (FieldDTO fieldDTO : sectionDTO.getFields()) {

                FormFields field = new FormFields();
                // preserve existing field ID if frontend sends it
                if (fieldDTO.getId() != null && !fieldDTO.getId().isBlank()) {
                    field.setId(UUID.fromString(fieldDTO.getId()));
                }

                try {
                    field.setFieldType(FieldType.valueOf(fieldDTO.getFieldType().toUpperCase()));
                } catch (Exception e) {
                    throw new RuntimeException("Invalid field type: " + fieldDTO.getFieldType());
                }

                field.setFieldOrder(fieldDTO.getFieldOrder());

                field.setFieldConfig(
                        fieldDTO.getFieldConfig() != null ? fieldDTO.getFieldConfig() : new HashMap<>()
                );

                field.setFieldStyle(
                        fieldDTO.getFieldStyle() != null ? fieldDTO.getFieldStyle() : new HashMap<>()
                );

                field.setQuizConfig(fieldDTO.getQuizConfig());
                field.setFieldLogic(fieldDTO.getFieldLogic());

                field.setSection(section);

                fields.add(field);
            }

            section.setFields(fields);
            form.getSections().add(section);
        }

        formRepository.saveAndFlush(form);

        return true;
    }

    private FormGetDTO convertToDTO(Form form) {
        FormGetDTO dto = new FormGetDTO();
        dto.setId(form.getId());
        dto.setTheme(form.getTheme());
        dto.setTitle(form.getTitle());
        dto.setDescription(form.getDescription());
        dto.setSettings(form.getSettings());
        dto.setPublished(form.isPublished());
        dto.setCreatedAt(form.getCreatedAt());
        dto.setCreatedBy(form.getUser().getUsername());
        dto.setMainParentId(form.getMainParentId());
        dto.setVersionId(form.getVersionId());
        dto.setEditable(form.isEditable());

        double maxScore = calculateMaxScore(form.getId());
        dto.setMaxScore(maxScore);

        if (form.getSections() != null) {
            dto.setSections(form.getSections().stream().map(section -> {
                SectionDTO sectionDTO = new SectionDTO();
                sectionDTO.setId(section.getId().toString());
                sectionDTO.setSectionTitle(section.getSectionTitle());
                sectionDTO.setSectionOrder(section.getSectionOrder());

                if (section.getFields() != null) {
                    sectionDTO.setFields(section.getFields().stream().map(field -> {
                        FieldDTO fieldDTO = new FieldDTO();
                        fieldDTO.setId(field.getId().toString());
                        fieldDTO.setFieldType(field.getFieldType().name());
                        fieldDTO.setFieldOrder(field.getFieldOrder());
                        fieldDTO.setFieldStyle(field.getFieldStyle());
                        fieldDTO.setFieldConfig(field.getFieldConfig());
                        fieldDTO.setQuizConfig(field.getQuizConfig());
                        fieldDTO.setFieldLogic(field.getFieldLogic());
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

    @Transactional
    public void softDeleteForm(String username, UUID id) {

        Form form = formRepository.findFormByIdAndUsername(id, username)
                .orElseThrow(() -> new RuntimeException("Form not found or not authorized"));

        if (form.isDeleted()) return;

        boolean wasPublished = form.isPublished();

        // Step 1: delete current
        form.setDeleted(true);
        form.setPublished(false); // IMPORTANT
        formRepository.save(form);

        // Step 2: If deleted form was ACTIVE → promote latest draft
        if (wasPublished) {

            UUID parentId = form.getMainParentId() != null
                    ? form.getMainParentId()
                    : form.getId();

            List<Form> versions = formRepository
                    .findByMainParentIdOrderByVersionIdDesc(parentId)
                    .stream()
                    .filter(f -> !f.isDeleted())
                    .collect(Collectors.toList());

            if (!versions.isEmpty()) {
                Form latest = versions.get(0); // highest versionId
                latest.setPublished(true);
                formRepository.save(latest);
            }
        }
    }

    public List<FormGetDTO> getTrashedForms(String username) {
        List<Form>forms = formRepository.findFormsByUsernameWithSections(username);

        forms = forms.stream().filter(Form::isDeleted).toList();

        if (forms.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> formIds = forms.stream().map(Form::getId).toList();
        formSectionRepository.findByFormIdInWithFields(formIds);

       /* return forms.stream()
                .map(this::convertToDTO)
                .toList();*/

        return forms.stream()
                .map(form -> {
                    FormGetDTO dto = convertToDTO(form);
                    long responseCount = formResponseRepository.countByForm_Id(form.getId());
                    dto.setTotalResponses(responseCount);
                    return dto;
                })
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

    @Transactional(readOnly = true)
    public List<VersionResponseDTO> getVersions(UUID formId) {

        // Get form
        Form form = formRepository.findById(formId)
                .orElseThrow(() -> new RuntimeException("Form not found"));

        //Resolve parentId correctly
        UUID parentId = form.getMainParentId() != null
                ? form.getMainParentId()
                : form.getId();

        //Get all versions
        List<Form> forms = formRepository
                .findByMainParentIdOrderByVersionIdDesc(parentId);

        //REMOVE deleted forms
        forms = forms.stream()
                .filter(f -> !f.isDeleted())
                .collect(Collectors.toList());

        // Convert to DTO
        return forms.stream().map(v -> {
            VersionResponseDTO dto = new VersionResponseDTO();
            dto.setVersionId(v.getVersionId());
            dto.setFormName(v.getTitle());
            dto.setFormId(v.getId());
            dto.setPublished(v.isPublished());
            dto.setCreatedAt(v.getCreatedAt());
            dto.setEditable(v.isEditable());
            return dto;
        }).collect(Collectors.toList());
    }
    @Transactional
    public void switchVersion(UUID formId, int versionId, String username) {

        Form current = formRepository.findFormByIdAndUsername(formId, username)
                .orElseThrow(() -> new RuntimeException("Form not found or not authorized"));

        UUID parentId = current.getMainParentId();

        // deactivate all versions
        formRepository.deactivateAllVersions(parentId);

        // directly fetch selected version (NO STREAM)
        Form selected = formRepository.findByMainParentIdAndVersionId(parentId, versionId)
                .orElseThrow(() -> new RuntimeException("Version not found"));

        selected.setPublished(true);

        formRepository.save(selected);
    }
    @Transactional
    public void deleteAllVersions(UUID formId) {
        Form form = formRepository.findById(formId)
                .orElseThrow(() -> new RuntimeException("Form not found"));
        UUID parentId = form.getMainParentId() != null ? form.getMainParentId() : form.getId();
        formRepository.softDeleteByMainParentId(parentId);
    }
    private double calculateMaxScore(UUID formId) {
        double maxScore = 0;

        List<FormSection> sections =
                formSectionRepository.findByFormIdInWithFields(List.of(formId));

        for (FormSection section : sections) {

            for (FormFields field : section.getFields()) {

                Map<String, Object> quizConfig = field.getQuizConfig();

                if (quizConfig == null || quizConfig.isEmpty()) {
                    continue;
                }

                Object isScoredObj = quizConfig.get("isScored");

                boolean isScored =
                        isScoredObj instanceof Boolean b
                                ? b
                                : isScoredObj != null && Boolean.parseBoolean(isScoredObj.toString());

                if (!isScored) {
                    continue;
                }

                Object pointsObj = quizConfig.get("points");

                double points = 0.0;

                if (pointsObj instanceof Number n) {
                    points = n.doubleValue();
                } else if (pointsObj != null) {
                    try {
                        points = Double.parseDouble(pointsObj.toString());
                    } catch (Exception ignored) {
                        points = 0.0;
                    }
                }

                maxScore += points;
            }
        }

        return maxScore;
    }

    @Transactional
    private void deleteEditor(UUID formId){
        List<UserFormRole> editorRoles = userFormRoleRepository.findByFormIdAndRole(formId, RoleType.EDITOR);
        if (!editorRoles.isEmpty()) {
            userFormRoleRepository.deleteAll(editorRoles);
        }
    }
}