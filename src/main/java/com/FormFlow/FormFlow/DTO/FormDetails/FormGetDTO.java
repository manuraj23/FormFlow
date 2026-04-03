package com.FormFlow.FormFlow.DTO.FormDetails;

import com.FormFlow.FormFlow.DTO.FormDetails.SectionDTO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class FormGetDTO {

    private UUID id;
    private String title;
    private String description;
    private boolean published;

    private String createdBy;

    private List<SectionDTO> sections;
    private LocalDateTime createdAt;
}
