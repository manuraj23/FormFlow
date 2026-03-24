package com.FormFlow.FormFlow.Service;

import com.FormFlow.FormFlow.Entity.*;
import com.FormFlow.FormFlow.DTO.*;
import com.FormFlow.FormFlow.enums.FieldType;
import com.FormFlow.FormFlow.Repository.FormRepository;
import com.FormFlow.FormFlow.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateFormService {

    private final FormRepository formRepository;
    private final UserRepository userRepository;

    public String createForm(FormCreateDTO dto) {

        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Form form = new Form();
        form.setTitle(dto.getTitle());
        form.setDescription(dto.getDescription());
        form.setPublished(dto.isPublished());
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
                        field.setFieldType(
                                FieldType.valueOf(fieldDTO.getFieldType().toUpperCase())
                        );
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

        return "Form Created Successfully";
    }
}