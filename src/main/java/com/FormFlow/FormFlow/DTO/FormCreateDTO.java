package com.FormFlow.FormFlow.DTO;

import lombok.Data;
import java.util.List;

@Data
public class FormCreateDTO {

    private String title;
    private String description;
    private boolean published;
    private Long userId;

    private List<SectionDTO> sections;
}