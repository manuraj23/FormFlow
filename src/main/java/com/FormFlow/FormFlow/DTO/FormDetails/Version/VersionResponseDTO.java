package com.FormFlow.FormFlow.DTO.FormDetails.Version;

import lombok.Data;

import java.util.UUID;
@Data
public class VersionResponseDTO {
    private int versionId;
    private String formName;
    private UUID FormId;
}
