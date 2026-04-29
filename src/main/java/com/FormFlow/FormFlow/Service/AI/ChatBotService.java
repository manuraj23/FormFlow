package com.FormFlow.FormFlow.Service.AI;

import com.FormFlow.FormFlow.DTO.FormDetails.FieldDTO;
import com.FormFlow.FormFlow.DTO.FormDetails.FormCreateDTO;
import com.FormFlow.FormFlow.DTO.FormDetails.SectionDTO;
import com.FormFlow.FormFlow.Service.User.UserService;
import com.FormFlow.FormFlow.enums.FieldType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class ChatBotService {

    @Value("${ai.gemini.api-key}")
    private String geminiApiKey;

    @Value("${ai.gemini.api-base:https://generativelanguage.googleapis.com/v1beta}")
    private String geminiApiBase;

    @Value("${ai.gemini.model:gemini-3-flash-preview}")
    private String geminiModel;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserService userService;

    // ===================== MAIN API =====================
    public ResponseEntity<?> generateForm(Map<String, String> request) {

        try {
            String userPrompt = request.get("prompt");
            if (userPrompt == null || userPrompt.isBlank()) {
                return ResponseEntity.badRequest().body("Prompt is required");
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication is required");
            }

            String username = authentication.getName();

            UUID formId = generateFormId(userPrompt, username);

            return ResponseEntity.ok(Map.of("formId", formId));

        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Gemini API failed: " + e.getStatusCode().value());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    public UUID generateFormFromDraft(Map<String, Object> draft, String username) {
        String userPrompt;
        try {
            userPrompt = objectMapper.writeValueAsString(draft == null ? Map.of() : draft);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize draft form data: " + e.getMessage(), e);
        }

        return generateFormId("Create a final form from this conversation draft:\n" + userPrompt, username);
    }

    private UUID generateFormId(String userPrompt, String username) {

        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new IllegalStateException("Gemini API key is not configured");
        }

        String systemPrompt = """
You are a form builder assistant.
Generate a JSON structure strictly matching the backend schema.

Rules:
- Output ONLY valid JSON
- Do NOT include explanations or extra text
- Follow the exact structure below

Top-level object:
{
  "title": string,
  "description": string,
  "published": false,
  "theme": string,
  "settings": object,
  "sections": []
}

Sections:
- Each section must include:
  - sectionTitle (string)
  - sectionOrder (number starting from 1)
  - fields (array)

Fields:
- Each field must include:
  - fieldType (must be one of: TEXT, TEXTAREA, DROPDOWN, CHECKBOX, FILE, RADIO)
  - fieldOrder (number starting from 1 within each section)
  - fieldConfig (object)
  - fieldStyle (object)
  - quizConfig (object, optional)
  - fieldLogic (object, optional)

FieldConfig rules:
- Must include:
  - label (string)
  - required (boolean)
- Optional properties:
  - placeholder (string)
  - options (array of strings)
  - min / max
  - defaultValue

FieldStyle rules:
- Keep it minimal (can be empty {})
- Optional keys: width, color, fontSize

Important Constraints:
- Ensure proper ordering using sectionOrder and fieldOrder
- Use MULTI_SELECT when multiple choices are allowed
- Use appropriate field types based on field meaning
- Mark important fields as required = true
- Ensure options are provided for selection-based fields
""";

        String finalPrompt = systemPrompt + "\nUser Request: " + userPrompt;

        Map<String, Object> part = Map.of("text", finalPrompt);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> body = Map.of("contents", List.of(content));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String url = String.format("%s/models/%s:generateContent?key=%s",
                geminiApiBase, geminiModel, geminiApiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> responseBody = Objects.requireNonNull(
                    response.getBody(),
                    "Gemini API returned an empty response"
            );
            if (responseBody.get("candidates") == null) {
                throw new RuntimeException("Gemini API returned an empty response");
            }

            List<?> candidates = (List<?>) responseBody.get("candidates");

            Map<?, ?> first = (Map<?, ?>) candidates.get(0);
            Map<?, ?> contentMap = (Map<?, ?>) first.get("content");
            List<?> parts = (List<?>) contentMap.get("parts");
            Map<?, ?> textPart = (Map<?, ?>) parts.get(0);

            String result = (String) textPart.get("text");

            FormCreateDTO dto = convertToCreateDTO(result);

            return userService.createForm(dto, username);

        } catch (HttpStatusCodeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error: " + e.getMessage(), e);
        }
    }

    // ===================== DTO CONVERSION =====================
    private FormCreateDTO convertToCreateDTO(String rawJson) {
        try {
            String normalizedJson = normalizeJson(rawJson);
            JsonNode root = objectMapper.readTree(normalizedJson);

            FormCreateDTO dto = new FormCreateDTO();
            dto.setTitle(defaultIfBlank(readText(root, "title"), "Untitled Form"));
            dto.setDescription(defaultIfBlank(readText(root, "description"), "Generated by AI"));
            dto.setTheme(defaultIfBlank(readText(root, "theme"), "default"));
            dto.setPublished(false);

            if (root.get("settings") != null) {
                dto.setSettings(toMap(root.get("settings")));
            } else {
                dto.setSettings(new HashMap<>());
            }

            List<SectionDTO> sections = new ArrayList<>();

            JsonNode sectionsNode = root.get("sections");
            if (sectionsNode != null && sectionsNode.isArray()) {

                for (int i = 0; i < sectionsNode.size(); i++) {

                    JsonNode sectionNode = sectionsNode.get(i);
                    SectionDTO sectionDTO = new SectionDTO();

                    sectionDTO.setSectionTitle(defaultIfBlank(readText(sectionNode, "sectionTitle"), "Section " + (i + 1)));
                    sectionDTO.setSectionOrder(parseIntOrDefault(sectionNode.get("sectionOrder"), i + 1));

                    List<FieldDTO> fields = new ArrayList<>();

                    JsonNode fieldsNode = sectionNode.get("fields");
                    if (fieldsNode != null && fieldsNode.isArray()) {

                        for (int j = 0; j < fieldsNode.size(); j++) {

                            JsonNode fieldNode = fieldsNode.get(j);
                            FieldDTO fieldDTO = new FieldDTO();

                            fieldDTO.setFieldType(normalizeFieldType(readText(fieldNode, "fieldType")));
                            fieldDTO.setFieldOrder(parseIntOrDefault(fieldNode.get("fieldOrder"), j + 1));

                            fieldDTO.setFieldConfig(
                                    fieldNode.get("fieldConfig") != null
                                            ? toMap(fieldNode.get("fieldConfig"))
                                            : new HashMap<>()
                            );

                            fieldDTO.setFieldStyle(
                                    fieldNode.get("fieldStyle") != null
                                            ? toMap(fieldNode.get("fieldStyle"))
                                            : new HashMap<>()
                            );

                            fieldDTO.setQuizConfig(
                                    fieldNode.get("quizConfig") != null
                                            ? toMap(fieldNode.get("quizConfig"))
                                            : new HashMap<>()
                            );

                            fieldDTO.setFieldLogic(
                                    fieldNode.get("fieldLogic") != null
                                            ? toMap(fieldNode.get("fieldLogic"))
                                            : new HashMap<>()
                            );

                            fields.add(fieldDTO);
                        }
                    }

                    if (fields.isEmpty()) {
                        fields.add(defaultField());
                    }

                    sectionDTO.setFields(fields);
                    sections.add(sectionDTO);
                }
            }

            if (sections.isEmpty()) {
                sections.add(defaultSection());
            }

            dto.setSections(sections);
            return dto;

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI JSON: " + e.getMessage());
        }
    }

    // ===================== HELPERS =====================
    private String normalizeJson(String rawJson) {
        String cleaned = rawJson.trim();

        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replace("```json", "").replace("```", "");
        }

        int start = cleaned.indexOf("{");
        int end = cleaned.lastIndexOf("}");

        return cleaned.substring(start, end + 1);
    }

    private String readText(JsonNode node, String field) {
        return node.get(field) != null ? node.get(field).asText() : null;
    }

    private int parseIntOrDefault(JsonNode node, int fallback) {
        return node != null && node.canConvertToInt() ? node.asInt() : fallback;
    }

    private String normalizeFieldType(String fieldType) {
        if (fieldType == null || fieldType.isBlank()) {
            return FieldType.TEXT.name();
        }

        String normalized = fieldType.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        try {
            return FieldType.valueOf(normalized).name();
        } catch (Exception ignored) {
            return FieldType.TEXT.name();
        }
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private SectionDTO defaultSection() {
        SectionDTO section = new SectionDTO();
        section.setSectionTitle("Section 1");
        section.setSectionOrder(1);
        section.setFields(List.of(defaultField()));
        return section;
    }

    private FieldDTO defaultField() {
        FieldDTO field = new FieldDTO();
        field.setFieldType(FieldType.TEXT.name());
        field.setFieldOrder(1);
        field.setFieldConfig(new HashMap<>(Map.of("label", "Untitled Field", "required", false)));
        field.setFieldStyle(new HashMap<>());
        field.setQuizConfig(new HashMap<>());
        field.setFieldLogic(new HashMap<>());
        return field;
    }

    private Map<String, Object> toMap(JsonNode node) {
        return objectMapper.convertValue(node, new TypeReference<>() {});
    }


    public String callGeminiRaw(String prompt) {

        int maxRetries = 3;
        int delay = 800;

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {

                Map<String, Object> part = Map.of("text", prompt);
                Map<String, Object> content = Map.of("parts", List.of(part));
                Map<String, Object> body = Map.of("contents", List.of(content));

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                String url = String.format("%s/models/%s:generateContent?key=%s",
                        geminiApiBase, geminiModel, geminiApiKey);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        new ParameterizedTypeReference<>() {}
                );

                Map<String, Object> responseBody = Objects.requireNonNull(response.getBody());

                List<?> candidates = (List<?>) responseBody.get("candidates");

                Map<?, ?> first = (Map<?, ?>) candidates.get(0);
                Map<?, ?> contentMap = (Map<?, ?>) first.get("content");
                List<?> parts = (List<?>) contentMap.get("parts");
                Map<?, ?> textPart = (Map<?, ?>) parts.get(0);

                return (String) textPart.get("text");

            } catch (HttpStatusCodeException e) {

                if (e.getStatusCode().value() == 503 && attempt < maxRetries - 1) {
                    try {
                        Thread.sleep(delay);
                        delay *= 2;
                    } catch (InterruptedException ignored) {}
                } else {
                    throw e;
                }
            }
        }

        throw new RuntimeException("Gemini API failed after retries");
    }
}