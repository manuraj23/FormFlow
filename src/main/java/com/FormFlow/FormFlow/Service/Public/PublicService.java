package com.FormFlow.FormFlow.Service.Public;

import com.FormFlow.FormFlow.DTO.FormDetails.FieldDTO;
import com.FormFlow.FormFlow.DTO.FormDetails.FormGetDTO;
import com.FormFlow.FormFlow.DTO.FormDetails.SectionDTO;
import com.FormFlow.FormFlow.Entity.Form;
import com.FormFlow.FormFlow.Repository.FormRepository;
import com.FormFlow.FormFlow.Repository.FormSectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PublicService {

    @Autowired
    public FormRepository formRepository;

    @Autowired
    public FormSectionRepository formSectionRepository;

    @Transactional(readOnly = true)
    public FormGetDTO getFormById(UUID id) {

        Form form = formRepository.findById(id)
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
                        return fieldDTO;
                    }).toList());
                }

                return sectionDTO;
            }).toList());
        }

        return dto;
    }
}
