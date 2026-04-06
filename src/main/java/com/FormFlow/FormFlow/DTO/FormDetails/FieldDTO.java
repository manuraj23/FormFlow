package com.FormFlow.FormFlow.DTO.FormDetails;

import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class FieldDTO {

    private UUID id;
    private String fieldType;
    private int fieldOrder;
    private Map<String, Object> fieldStyle;
    private Map<String, Object> fieldConfig;
}
