package com.FormFlow.FormFlow.DTO.FormDetails;

import com.FormFlow.FormFlow.DTO.FormDetails.SectionDTO;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class FormCreateDTO {

    private String theme;
    private String title;
    private String description;
    private boolean published;
    private UUID userId;
    private int versionId;
    private UUID mainParentId;

    private Map<String, Object> settings;
    private List<SectionDTO> sections;
}
