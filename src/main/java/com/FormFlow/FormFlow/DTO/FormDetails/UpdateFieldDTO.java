package com.FormFlow.FormFlow.DTO.FormDetails;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class UpdateFieldDTO {
    private String label;
    private String placeholder;
    private List<String> options;
    private Map<String, Object> validations;

    private String color;
    private String fontSize;
    private boolean bold;
    private boolean italic;
    private boolean underline;

    private Map<String, Object> quizConfig;
    private Map<String, Object> fieldLogic;
}
