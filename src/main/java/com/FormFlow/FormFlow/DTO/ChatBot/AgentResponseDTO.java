package com.FormFlow.FormFlow.DTO.ChatBot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgentResponseDTO {
    private String type;
    private String message;
    private UUID sessionId;
    private UUID formId;
    private boolean complete;
}
