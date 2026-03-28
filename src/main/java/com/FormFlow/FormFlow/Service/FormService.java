package com.FormFlow.FormFlow.Service;

import com.FormFlow.FormFlow.DTO.FormDetails.FieldDTO;
import com.FormFlow.FormFlow.DTO.FormDetails.FormGetDTO;
import com.FormFlow.FormFlow.DTO.FormDetails.SectionDTO;
import com.FormFlow.FormFlow.Entity.Form;
import com.FormFlow.FormFlow.Entity.FormFields;
import com.FormFlow.FormFlow.Entity.FormSection;
import com.FormFlow.FormFlow.Repository.FormRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FormService {

    private final FormRepository formRepository;

    public FormService(FormRepository formRepository) {
        this.formRepository = formRepository;
    }

    // Converts a FormFields entity into a FieldDTO
    // We do this so we never expose the raw entity directly to the frontend
    private FieldDTO toFieldDTO(FormFields field) {
        FieldDTO dto = new FieldDTO();
        dto.setFieldType(String.valueOf(field.getFieldType()));
        dto.setFieldOrder(field.getFieldOrder());
        dto.setFieldConfig(field.getFieldConfig());
        return dto;
    }

    // Converts a FormSection entity into a SectionDTO
    // Also converts each field inside that section using toFieldDTO
    private SectionDTO toSectionDTO(FormSection section) {
        SectionDTO dto = new SectionDTO();
        dto.setSectionTitle(section.getSectionTitle());
        dto.setSectionOrder(section.getSectionOrder());
        dto.setFields(
                section.getFields()
                        .stream()
                        .map(this::toFieldDTO)
                        .collect(Collectors.toList())
        );
        return dto;
    }

    // Converts a full Form entity into a FormGetDTO
    // Also converts each section inside that form using toSectionDTO
    private FormGetDTO toFormGetDTO(Form form) {
        FormGetDTO dto = new FormGetDTO();
        dto.setId(form.getId());
        dto.setTitle(form.getTitle());
        dto.setDescription(form.getDescription());
        dto.setPublished(form.isPublished());

        // Gets the name of the user who created the form
        // null check in case user is not set
//        dto.setCreatedBy(form.getUser() != null ? form.getUser().getName() : null);

        dto.setSections(
                form.getSections()
                        .stream()
                        .map(this::toSectionDTO)
                        .collect(Collectors.toList())
        );
        return dto;
    }

    // GET form by ID
    // Finds the form, converts it to DTO, returns it
    // orElseThrow handles the case where no form exists with that ID
    public FormGetDTO getFormById(Long id) {
        Form form = formRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Form not found with id: " + id));
        return toFormGetDTO(form);
    }

    // GET forms by status
    // published=true means PUBLISHED, published=false means DRAFT
    // converts each form in the list to a DTO
    public List<FormGetDTO> getFormsByStatus(boolean published) {
        return formRepository.findByPublished(published)
                .stream()
                .map(this::toFormGetDTO)
                .collect(Collectors.toList());
    }
}