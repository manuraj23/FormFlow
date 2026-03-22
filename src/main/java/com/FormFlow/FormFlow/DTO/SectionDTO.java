package com.FormFlow.FormFlow.DTO;

import lombok.Data;
import java.util.List;

@Data
public class SectionDTO {

    private String sectionTitle;
    private int sectionOrder;

    private List<FieldDTO> fields;
}