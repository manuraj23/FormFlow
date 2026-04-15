package com.FormFlow.FormFlow.DTO.Response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
public class FormResponseDTO {

    private UUID responseId;
    private UUID formId;
    private Map<String, Object> response;
    private LocalDateTime submittedAt;
    private Double score;
    private Map<String, Object> evaluation;
}
