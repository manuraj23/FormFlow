package com.FormFlow.FormFlow.Service.AI;

import com.FormFlow.FormFlow.DTO.FormDetails.FieldDTO;
import com.FormFlow.FormFlow.DTO.FormDetails.FormGetDTO;
import com.FormFlow.FormFlow.DTO.FormDetails.SectionDTO;
import com.FormFlow.FormFlow.Entity.*;
import com.FormFlow.FormFlow.Repository.FormRepository;
import com.FormFlow.FormFlow.Repository.UserRepository;
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

    @Value("${ai.gemini.model:gemini-2.5-flash}")
    private String geminiModel;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FormRepository formRepository;

    // ===================== MAIN API =====================
    public ResponseEntity<?> generateForm(Map<String, String> request) {

        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Gemini API key is not configured");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        String userPrompt = request.get("prompt");
        if (userPrompt == null || userPrompt.isBlank()) {
            return ResponseEntity.badRequest().body("Prompt is required");
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
  - fieldType (must be one of: TEXT, EMAIL, TEXTAREA, NUMBER, PHONE, DATE, TIME, DROPDOWN, RADIO, CHECKBOX, MULTI_SELECT, FILE, RATING)
  - fieldOrder (number starting from 1 within each section)
  - fieldConfig (object)
  - fieldStyle (object)

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

            Map<String, Object> responseBody = response.getBody();
            List<?> candidates = (List<?>) responseBody.get("candidates");

            Map<?, ?> first = (Map<?, ?>) candidates.get(0);
            Map<?, ?> contentMap = (Map<?, ?>) first.get("content");
            List<?> parts = (List<?>) contentMap.get("parts");
            Map<?, ?> textPart = (Map<?, ?>) parts.get(0);

            String result = (String) textPart.get("text");

            FormGetDTO dto = convertToDTO(result);

            UUID formId = saveForm(dto, username);

            return ResponseEntity.ok(Map.of("formId", formId));

        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Gemini API failed: " + e.getStatusCode().value());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    // ===================== SAVE LOGIC =====================
    private UUID saveForm(FormGetDTO dto, String username) {

        User user = userRepository.findByUsername(username);

        Form form = new Form();
        form.setTitle(dto.getTitle());
        form.setDescription(dto.getDescription());
        form.setTheme(dto.getTheme());

        form.setPublished(false);

        form.setSettings(dto.getSettings());
        form.setDeleted(false);
        form.setUser(user);

        if (dto.getSections() != null) {
            List<FormSection> sections = dto.getSections().stream().map(sectionDTO -> {

                FormSection section = new FormSection();
                section.setSectionTitle(sectionDTO.getSectionTitle());
                section.setSectionOrder(sectionDTO.getSectionOrder());
                section.setForm(form);

                if (sectionDTO.getFields() != null) {
                    List<FormFields> fields = sectionDTO.getFields().stream().map(fieldDTO -> {

                        FormFields field = new FormFields();

                        field.setFieldType(
                                FieldType.valueOf(fieldDTO.getFieldType().toUpperCase())
                        );

                        field.setFieldOrder(fieldDTO.getFieldOrder());
                        field.setFieldConfig(fieldDTO.getFieldConfig());
                        field.setFieldStyle(fieldDTO.getFieldStyle());
                        field.setSection(section);

                        return field;
                    }).toList();

                    section.setFields(fields);
                }

                return section;
            }).toList();

            form.setSections(sections);
        }

        Form savedForm = formRepository.save(form);
        return savedForm.getId();
    }

    // ===================== DTO CONVERSION =====================
    private FormGetDTO convertToDTO(String rawJson) {
        try {
            String normalizedJson = normalizeJson(rawJson);
            JsonNode root = objectMapper.readTree(normalizedJson);

            FormGetDTO dto = new FormGetDTO();
            dto.setTitle(readText(root, "title"));
            dto.setDescription(readText(root, "description"));
            dto.setTheme(readText(root, "theme"));
            dto.setPublished(false);

            if (root.get("settings") != null) {
                dto.setSettings(toMap(root.get("settings")));
            }

            List<SectionDTO> sections = new ArrayList<>();

            JsonNode sectionsNode = root.get("sections");
            if (sectionsNode != null && sectionsNode.isArray()) {

                for (int i = 0; i < sectionsNode.size(); i++) {

                    JsonNode sectionNode = sectionsNode.get(i);
                    SectionDTO sectionDTO = new SectionDTO();

                    sectionDTO.setSectionTitle(readText(sectionNode, "sectionTitle"));
                    sectionDTO.setSectionOrder(i + 1);

                    List<FieldDTO> fields = new ArrayList<>();

                    JsonNode fieldsNode = sectionNode.get("fields");
                    if (fieldsNode != null && fieldsNode.isArray()) {

                        for (int j = 0; j < fieldsNode.size(); j++) {

                            JsonNode fieldNode = fieldsNode.get(j);
                            FieldDTO fieldDTO = new FieldDTO();

                            fieldDTO.setFieldType(readText(fieldNode, "fieldType"));
                            fieldDTO.setFieldOrder(j + 1);

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

                            fields.add(fieldDTO);
                        }
                    }

                    sectionDTO.setFields(fields);
                    sections.add(sectionDTO);
                }
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
            cleaned = cleaned.replaceAll("```json", "").replaceAll("```", "");
        }

        int start = cleaned.indexOf("{");
        int end = cleaned.lastIndexOf("}");

        return cleaned.substring(start, end + 1);
    }

    private String readText(JsonNode node, String field) {
        return node.get(field) != null ? node.get(field).asText() : null;
    }

    private Map<String, Object> toMap(JsonNode node) {
        return objectMapper.convertValue(node, new TypeReference<>() {});
    }
}