package com.FormFlow.FormFlow.Service.AI;

import com.FormFlow.FormFlow.DTO.ChatBot.AgentResponseDTO;
import com.FormFlow.FormFlow.Entity.ChatBot.ChatSession;
import com.FormFlow.FormFlow.Repository.ChatBot.ChatSessionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AgentService {

    @Autowired
    private ChatSessionRepository sessionRepository;

    @Autowired
    private ChatBotService chatBotService;

    private final ObjectMapper mapper = new ObjectMapper();

    public AgentResponseDTO handleChat(UUID sessionId, String message) {

        AgentResponseDTO invalid = validateIncomingMessage(message);
        if (invalid != null) {
            return invalid;
        }

        try {
            ChatSession session = resolveSession(sessionId, message);

            if ("DONE".equalsIgnoreCase(session.getCurrentStep())) {
                return doneResponse(session, session.getCollectedData());
            }

            Map<String, Object> collectedData = new HashMap<>(Optional.ofNullable(session.getCollectedData())
                    .orElseGet(HashMap::new));

            String prompt = buildAgentPrompt(session.getGoal(), collectedData, message);
            Map<String, Object> action = parseJson(chatBotService.callGeminiRaw(prompt));

            return processAction(action, session, collectedData);

        } catch (Exception e) {
            AgentResponseDTO response = new AgentResponseDTO();
            response.setType("error");
            response.setMessage(e.getMessage());
            response.setComplete(false);
            return response;
        }
    }

    private ChatSession resolveSession(UUID sessionId, String message) {
        if (sessionId == null) {
            ChatSession session = new ChatSession();
            session.setGoal(message);
            session.setCollectedData(new HashMap<>());
            session.setCurrentStep("COLLECTING");
            return sessionRepository.save(session);
        }

        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
    }

    private AgentResponseDTO processAction(Map<String, Object> action, ChatSession session, Map<String, Object> collectedData) {

        String actionType = stringValue(action.get("action"));
        Map<String, Object> draft = mapValue(action.get("draft"));
        if (!draft.isEmpty()) {
            collectedData.clear();
            collectedData.putAll(draft);
        }

        if ("generate_form".equalsIgnoreCase(actionType)) {
            String username = currentUsername();
            if (username == null) {
                return errorResponse(session.getId(), "Authentication required to generate a form");
            }

            UUID formId = chatBotService.generateFormFromDraft(collectedData, username);
            collectedData.put("generatedFormId", formId.toString());

            session.setCollectedData(collectedData);
            session.setCurrentStep("DONE");
            sessionRepository.save(session);

            return new AgentResponseDTO("form", "Form generated successfully", session.getId(), formId, true);
        }

        String question = stringValue(action.get("question"));
        if (question == null || question.isBlank()) {
            question = defaultFollowUpQuestion(collectedData);
        }

        session.setCollectedData(collectedData);
        session.setCurrentStep("COLLECTING");
        sessionRepository.save(session);

        return new AgentResponseDTO("question", question, session.getId(), null, false);
    }

    // ================= PROMPT =================
    private String buildAgentPrompt(String goal,
                                    Map<String, Object> collectedData,
                                    String userMessage) {

        String draftJson = toJson(collectedData);

        return """
                You are an agentic form-building assistant.
                
                Your job is to collect any missing details through one concise follow-up question at a time, then switch to form generation once the draft is complete enough to build a valid backend form.
                
                Conversation goal:
                %s
                
                Current draft JSON:
                %s
                
                Latest user message:
                %s
                
                Return ONLY valid JSON with this shape:
                {
                  "action": "ask_question" | "generate_form",
                  "question": "one concise follow-up question or empty string",
                  "draft": {
                    "title": "string",
                    "description": "string",
                    "theme": "string",
                    "settings": {},
                    "sections": [
                      {
                        "sectionTitle": "string",
                        "sectionOrder": 1,
                        "fields": [
                          {
                            "fieldType": "TEXT",
                            "fieldOrder": 1,
                            "fieldConfig": {
                              "label": "string",
                              "required": true
                            },
                            "fieldStyle": {}
                          }
                        ]
                      }
                    ]
                  }
                }
                
                Rules:
                
                - Ask only one follow-up question at a time.
                - Carry forward all previously collected details into draft.
                - Use action generate_form only when the draft is complete enough to create the final form.
                - If the user already provided enough detail, return generate_form immediately.
                - Output JSON only; no markdown, no explanation.
                """.formatted(goal, draftJson, userMessage);
    }

    // ================= UTILS =================
    private Map<String, Object> parseJson(String json) {
        try {
            String normalized = normalizeJson(json);
            return mapper.readValue(normalized, new TypeReference<>() {
            });
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private String normalizeJson(String rawJson) {
        if (rawJson == null) {
            return "{}";
        }

        String cleaned = rawJson.trim();

        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replace("```json", "").replace("```", "");
        }

        int start = cleaned.indexOf("{");
        int end = cleaned.lastIndexOf("}");
        if (start < 0 || end < start) {
            return "{}";
        }

        return cleaned.substring(start, end + 1);
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private Map<String, Object> mapValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return mapper.convertValue(map, new TypeReference<>() {
            });
        }
        return new HashMap<>();
    }

    private String toJson(Object value) {
        try {
            return mapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private String defaultFollowUpQuestion(Map<String, Object> collectedData) {
        if (isBlank(collectedData.get("title"))) {
            return "What should be the form title?";
        }
        if (isBlank(collectedData.get("description"))) {
            return "What is the form description or purpose?";
        }
        if (isBlank(collectedData.get("sections"))) {
            return "How many sections should the form have, and what should the first section cover?";
        }
        return "What additional fields or section details should I add?";
    }

    private boolean isBlank(Object value) {
        return value == null || value.toString().isBlank();
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String username = authentication.getName();
        if (username == null || username.isBlank() || "anonymousUser".equals(username)) {
            return null;
        }

        return username;
    }

    private AgentResponseDTO errorResponse(UUID sessionId, String message) {
        AgentResponseDTO response = new AgentResponseDTO();
        response.setType("error");
        response.setMessage(message);
        response.setSessionId(sessionId);
        response.setComplete(false);
        return response;
    }

    private AgentResponseDTO validateIncomingMessage(String message) {
        if (message == null || message.isBlank()) {
            return errorResponse(null, "Message is required");
        }
        return null;
    }

    private AgentResponseDTO doneResponse(ChatSession session, Map<String, Object> collectedData) {
        AgentResponseDTO response = new AgentResponseDTO();
        response.setType("form");
        response.setMessage("This session already generated a form");
        response.setSessionId(session.getId());
        response.setComplete(true);

        Object formIdValue = collectedData == null ? null : collectedData.get("generatedFormId");
        if (formIdValue != null) {
            try {
                response.setFormId(UUID.fromString(formIdValue.toString()));
            } catch (Exception ignored) {
                // keep response without formId if the stored value is malformed
            }
        }

        return response;
    }
}


