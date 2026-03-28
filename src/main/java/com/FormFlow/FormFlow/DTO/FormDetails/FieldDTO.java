package com.FormFlow.FormFlow.DTO.FormDetails;

import lombok.Data;

import java.util.Map;

@Data
public class FieldDTO {

    private String fieldType;
    private int fieldOrder;
    private Map<String, Object> fieldConfig;
}
