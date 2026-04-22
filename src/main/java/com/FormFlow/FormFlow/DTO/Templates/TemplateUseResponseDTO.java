package com.FormFlow.FormFlow.DTO.Templates;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class TemplateUseResponseDTO {
    private UUID formId;
    private String message;
}

