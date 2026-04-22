package com.FormFlow.FormFlow.DTO;

import com.FormFlow.FormFlow.DTO.FormDetails.SectionDTO;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class UpdateFormDTO {

    private String theme;
    private String title;
    private String description;
    private Map<String, Object> settings;
    private List<SectionDTO> sections;
    private boolean published;

}
