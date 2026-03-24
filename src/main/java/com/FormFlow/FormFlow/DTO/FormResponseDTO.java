package com.FormFlow.FormFlow.DTO;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class FormResponseDTO {

    private Long responseId;
    private Long formId;
    private Map<String, Object> response;
    private LocalDateTime submittedAt;
}
