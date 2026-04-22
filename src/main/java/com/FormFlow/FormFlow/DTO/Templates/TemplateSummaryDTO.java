package com.FormFlow.FormFlow.DTO.Templates;

import lombok.Data;

import java.util.UUID;

@Data
public class TemplateSummaryDTO {
    private UUID id;
    private String templateName;
    private String title;
    private String description;
}

