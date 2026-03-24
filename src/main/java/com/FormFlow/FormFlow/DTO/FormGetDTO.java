package com.FormFlow.FormFlow.DTO;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class FormGetDTO {

    private Long id;
    private String title;
    private String description;
    private boolean published;

    private String createdBy;

    private List<SectionDTO> sections;
    private LocalDateTime createdAt;
}