package com.FormFlow.FormFlow.Service;

import com.FormFlow.FormFlow.Entity.Form;
import com.FormFlow.FormFlow.DTO.*;
import com.FormFlow.FormFlow.Repository.FormRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetFormService {

    private final FormRepository formRepository;

    public List<FormGetDTO> getAllForms() {

        return formRepository.findAll().stream().map(form -> {

            FormGetDTO dto = new FormGetDTO();
            dto.setId(form.getId());
            dto.setTitle(form.getTitle());
            dto.setDescription(form.getDescription());
            dto.setPublished(form.isPublished());
            dto.setCreatedAt(form.getCreatedAt());
//            dto.setCreatedBy(form.getUser().getName());

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
        }).toList();
    }
}