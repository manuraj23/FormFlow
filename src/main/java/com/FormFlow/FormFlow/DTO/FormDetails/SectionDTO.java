package com.FormFlow.FormFlow.DTO.FormDetails;

import com.FormFlow.FormFlow.DTO.FormDetails.FieldDTO;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class SectionDTO {

    private String id;
    private String sectionTitle;
    private int sectionOrder;
    private BigDecimal positiveMarks;
    private BigDecimal negativeMarks;
    private List<FieldDTO> fields;
}
