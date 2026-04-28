package com.FormFlow.FormFlow.Service.Public;

import com.FormFlow.FormFlow.DTO.FormDetails.FieldDTO;
import com.FormFlow.FormFlow.DTO.FormDetails.FormGetDTO;
import com.FormFlow.FormFlow.DTO.FormDetails.SectionDTO;
import com.FormFlow.FormFlow.Entity.Form;
import com.FormFlow.FormFlow.Entity.FormFields;
import com.FormFlow.FormFlow.Entity.FormSection;
import com.FormFlow.FormFlow.Repository.FormRepository;
import com.FormFlow.FormFlow.Repository.FormResponseRepository;
import com.FormFlow.FormFlow.Repository.FormSectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PublicService {

    @Autowired
    public FormRepository formRepository;

    @Autowired
    public FormSectionRepository formSectionRepository;

    @Autowired
    public FormResponseRepository formResponseRepository;

    @Transactional(readOnly = true)
    public FormGetDTO getFormById(UUID id) {

        Form form = formRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Form not found or not authorized"));

        if(form.isDeleted()) {
            throw new RuntimeException("Form not found or not authorized");
        }

        formSectionRepository.findByFormIdInWithFields(List.of(form.getId()));

       // return convertToDTO(form);
        FormGetDTO dto = convertToDTO(form);
        dto.setTotalResponses(formResponseRepository.countByForm_Id(id));
        return dto;
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


}
