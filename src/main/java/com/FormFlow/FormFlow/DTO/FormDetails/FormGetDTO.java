package com.FormFlow.FormFlow.DTO.FormDetails;

import com.FormFlow.FormFlow.DTO.FormDetails.SectionDTO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class FormGetDTO {

    private UUID id;
    private String theme;
    private String title;
    private String description;
    private boolean published;
    private Map<String, Object> settings;
    private String createdBy;

    private List<SectionDTO> sections;
    private LocalDateTime createdAt;
}
