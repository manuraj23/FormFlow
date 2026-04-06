package com.FormFlow.FormFlow.DTO.FormDetails;

import com.FormFlow.FormFlow.DTO.FormDetails.FieldDTO;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class SectionDTO {

    private UUID id;
    private String sectionTitle;
    private int sectionOrder;

    private List<FieldDTO> fields;
}
