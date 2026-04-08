package com.FormFlow.FormFlow.Service.Response;

import com.FormFlow.FormFlow.DTO.Response.FormResponseDTO;
import com.FormFlow.FormFlow.Entity.Form;
import com.FormFlow.FormFlow.Entity.FormFields;
import com.FormFlow.FormFlow.Entity.FormResponse;
import com.FormFlow.FormFlow.Entity.FormSection;
import com.FormFlow.FormFlow.Repository.FormRepository;
import com.FormFlow.FormFlow.Repository.FormResponseRepository;
import com.FormFlow.FormFlow.Repository.FormSectionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ResponseService {

    private final FormResponseRepository repository;
    private final FormSectionRepository formSectionRepository;
    private final FormRepository formRepository;

    // reads upload directory from application-dev.yml
    @Value("${file.upload-dir}")
    private String uploadDir;

    public ResponseService(FormResponseRepository repository,
                           FormSectionRepository formSectionRepository,
                           FormRepository formRepository) {
        this.repository = repository;
        this.formSectionRepository = formSectionRepository;
        this.formRepository = formRepository;
    }

    // updated to accept files alongside response data
    public FormResponseDTO saveResponse(FormResponseDTO dto,
                                        List<MultipartFile> files) {


        Map<String, String> uploadedFileMap = new HashMap<>();

        //validate form limits
        validateFormLimits(dto.getFormId());

        // validate
        validateResponse(dto.getFormId(), dto.getResponse(), files);

       // save files and fill map
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String savedFileName = saveFileToLocal(file);
                    uploadedFileMap.put(file.getOriginalFilename(), savedFileName);
                }
            }
        }
             // update response with urls
        Map<String, Object> responseMap = dto.getResponse();

        for (Map.Entry<String, Object> entry : new HashMap<>(responseMap).entrySet()) {

            // change_1 : making field ids the keys instead of labels
            String fieldId = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof String) {
                String fileName = (String) value;

                if (uploadedFileMap.containsKey(fileName)) {
                    String savedFileName = uploadedFileMap.get(fileName);
                    // String fileUrl = "http://localhost:8082/uploads/" + savedFileName; - not dynamic so removed
                    String fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                            .path("/uploads/")
                            .path(savedFileName)
                            .toUriString();

                    responseMap.put(fieldId, fileUrl);
                }
            }
        }


        // Save response to DB
        FormResponse entity = mapToEntity(dto);
        entity.setSubmittedAt(LocalDateTime.now());

        FormResponse saved = repository.save(entity);

        return mapToDTO(saved);
    }

    // saves file to local uploads/ folder and return a unique file name
    private String saveFileToLocal(MultipartFile file) {
        String uniqueFileName;
        String originalName;
        try {
            // create uploads directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // save file with a unique file name for each
            // REPLACE_EXISTING overwrites if same filename uploaded again
            // Path filePath = uploadPath.resolve(file.getOriginalFilename());
            originalName = file.getOriginalFilename();
            uniqueFileName = System.currentTimeMillis() + "_" + originalName;

            Path filePath = uploadPath.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), filePath,
                    StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {
            throw new RuntimeException("Could not save file: "
                    + file.getOriginalFilename() + ". Error: " + e.getMessage());
        }
        return uniqueFileName;
    }

    private void validateFormLimits(UUID formId) {

        Form form = formRepository.findById(formId)
                .orElseThrow(() -> new RuntimeException("Form not found"));

        Map<String, Object> settings = form.getSettings();
        if (settings == null) return;

        // checking deadline
        Object deadlineObj = settings.get("deadline");
        if (deadlineObj != null) {
            try {
                Instant deadline = Instant.parse(deadlineObj.toString());
                if (Instant.now().isAfter(deadline)) {
                    throw new RuntimeException("Form submission deadline has passed");
                }

            }catch (RuntimeException e) {
                throw e;
            }  catch (Exception e) {
                throw new RuntimeException("Invalid deadline format in form settings");
            }
        }

        //  max responses check
        Object maxResponsesObj = settings.get("maxResponses");
        if (maxResponsesObj != null) {
            int maxResponses = ((Number) maxResponsesObj).intValue();

            long currentCount = repository.countByFormId(formId);

            if (currentCount >= maxResponses) {
                throw new RuntimeException("Maximum response limit reached");
            }
        }
    }

    // validates all submitted response values against field validations
    private void validateResponse(UUID formId,
                                  Map<String, Object> submittedResponse,
                                  List<MultipartFile> files) {

       /* // build map of filename to MultipartFile for easy lookup during FILE validation
        Map<String, MultipartFile> fileMap = new HashMap<>();
        if (files != null) {
            for (MultipartFile file : files) {
                if (file.getOriginalFilename() != null) {
                    fileMap.put(file.getOriginalFilename(), file);
                }
            }
        } */

        // build map of filename to MultipartFile for easy lookup during FILE validation
        Map<String, MultipartFile> fileMap = new HashMap<>();
        if (files != null) {
            for (MultipartFile file : files) {
                if (file.getOriginalFilename() != null) {
                    // check for duplicate filenames first
                    if (fileMap.containsKey(file.getOriginalFilename())) {
                        throw new RuntimeException(
                                "Duplicate file name detected: " + file.getOriginalFilename()
                                        + ". Each uploaded file must have a unique name");
                    }
                    fileMap.put(file.getOriginalFilename(), file);
                }
            }
        }




        // fetch all sections with fields eagerly loaded in one query
        List<FormSection> sections = formSectionRepository
                .findByFormIdInWithFields(List.of(formId));
        // count FILE fields in this form
        long fileFieldCount = sections.stream()
                .flatMap(s -> s.getFields().stream())
                .filter(f -> f.getFieldType() != null &&
                        f.getFieldType().name().equals("FILE"))
                .count();

        // reject if more files uploaded than FILE fields exist
        if (files != null &&
                files.stream().filter(f -> !f.isEmpty()).count() > fileFieldCount) {
            throw new RuntimeException(
                    "Too many files uploaded. This form only accepts "
                            + fileFieldCount + " file(s)");
        }


        for (FormSection section : sections) {


            List<FormFields> fields = section.getFields();

            for (FormFields field : fields) {

                Map<String, Object> config = field.getFieldConfig();
                if (config == null) continue;

                String label = (String) config.get("label");
                if (label == null) continue;

                Map<String, Object> validations =
                        (Map<String, Object>) config.get("validations");

                List<String> options = (List<String>) config.get("options");

                // changed: looking up submitted value by field ID instead of label ***
                String fieldId = field.getId() != null ? field.getId().toString() : null;
                if (fieldId == null) continue;

                Object submittedValue = submittedResponse.get(fieldId);
                String valueStr = submittedValue != null
                        ? submittedValue.toString().trim() : null;

                if (validations == null || validations.isEmpty()) continue;

                // required check
                Object required = validations.get("required");
                if (Boolean.TRUE.equals(required)) {
                    if (valueStr == null || valueStr.isEmpty()) {
                        throw new RuntimeException(
                                "Field '" + label + "' is required");
                    }
                }

                if (valueStr == null || valueStr.isEmpty()) continue;

                // minLength check
                Object minLength = validations.get("minLength");
                if (minLength != null) {
                    int min = ((Number) minLength).intValue();
                    if (valueStr.length() < min) {
                        throw new RuntimeException(
                                "Field '" + label + "' must be at least "
                                        + min + " characters");
                    }
                }

                // maxLength check
                Object maxLength = validations.get("maxLength");
                if (maxLength != null) {
                    int max = ((Number) maxLength).intValue();
                    if (valueStr.length() > max) {
                        throw new RuntimeException(
                                "Field '" + label + "' must be at most "
                                        + max + " characters");
                    }
                }

                if (field.getFieldType() != null) {
                    switch (field.getFieldType().name()) {

                        case "TEXT":
                        case "TEXTAREA":
                            // email validation — email is a boolean key in validations
                            Object emailValidation = validations.get("email");
                            if (Boolean.TRUE.equals(emailValidation)) {
                                if (!valueStr.matches(
                                        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                                    throw new RuntimeException(
                                            "Field '" + label + "' must be a valid email address");
                                }
                            }
                            Object numberValidation = validations.get("number");
                            if (Boolean.TRUE.equals(numberValidation)) {
                                double parsedValue;
                                try {
                                    parsedValue = Double.parseDouble(valueStr);
                                } catch (NumberFormatException e) {
                                    throw new RuntimeException("Field '" + label + "' must be a valid number");
                                }
                                Object minVal = validations.get("min");
                                if (minVal != null && parsedValue < ((Number) minVal).doubleValue()) {
                                    throw new RuntimeException("Field '" + label + "' must be at least " + minVal);
                                }
                                Object maxVal = validations.get("max");
                                if (maxVal != null && parsedValue > ((Number) maxVal).doubleValue()) {
                                    throw new RuntimeException("Field '" + label + "' must be at most " + maxVal);
                                }
                            }
                            break;
                       /*


                        case "PHONE":
                            if (!valueStr.matches("^[+]?[0-9\\s\\-().]{7,15}$")) {
                                throw new RuntimeException(
                                        "Field '" + label + "' must be a valid phone number");
                            }
                            break;

                        case "DATE":
                            try {
                                java.time.LocalDate.parse(valueStr);
                            } catch (Exception e) {
                                throw new RuntimeException(
                                        "Field '" + label +
                                                "' must be a valid date in format YYYY-MM-DD");
                            }
                            break;



                        case "TIME":
                            if (!valueStr.matches(
                                    "^([01]?[0-9]|2[0-3]):[0-5][0-9](:[0-5][0-9])?$")) {
                                throw new RuntimeException(
                                        "Field '" + label +
                                                "' must be a valid time in format HH:MM");
                            }
                            break;
                          */
                        case "DROPDOWN":
                        case "RADIO":
                            if (options != null && !options.isEmpty()) {
                                if (!options.contains(valueStr)) {
                                    throw new RuntimeException(
                                            "Field '" + label + "' must be one of: " + options);
                                }
                            }
                            break;

                        case "CHECKBOX":
                            if (options != null && !options.isEmpty()) {
                                List<String> selected;
                                if (submittedValue instanceof List) {
                                    selected = (List<String>) submittedValue;
                                } else {
                                    selected = Arrays.asList(valueStr.split(","));
                                }
                                if (selected.isEmpty()) {
                                    Object requiredCheck = validations.get("required");
                                    if (Boolean.TRUE.equals(requiredCheck)) {
                                        throw new RuntimeException("Field '" + label + "' is required");
                                    }
                                }
                                for (String s : selected) {
                                    if (!options.contains(s.trim())) {
                                        throw new RuntimeException(
                                                "Field '" + label + "' contains invalid option: " + s.trim());
                                    }
                                }
                            }
                            break;
                        /*
                        case "MULTI_SELECT":
                            if (options != null && !options.isEmpty()) {
                                String[] selectedValues = valueStr.split(",");
                                for (String selected : selectedValues) {
                                    if (!options.contains(selected.trim())) {
                                        throw new RuntimeException(
                                                "Field '" + label +
                                                        "' contains invalid option: " + selected.trim());
                                    }
                                }
                            }
                            break;

                         */

                        case "FILE":

                            /* multiple files
                            if (files != null) {
                                long count = files.stream()
                                        .filter(file -> file.getOriginalFilename().equals(valueStr))
                                        .count();

                                if (count > 1) {
                                    throw new RuntimeException(
                                            "Multiple files uploaded for field '" + label );
                                }
                            }  */

                            MultipartFile uploadedFile = fileMap.get(valueStr);
                            // required validations
                            Object requiredFile = validations.get("required");
                            if (Boolean.TRUE.equals(requiredFile)) {


                                if (uploadedFile == null || uploadedFile.isEmpty()) {
                                    throw new RuntimeException(
                                            "Field '" + label + "' file is required");
                                }
                            }
                            // check file extension using fileType from validations
                            Object fileTypeObj = validations.get("fileType");
                            if (fileTypeObj != null && !fileTypeObj.toString().trim().isEmpty()) {
                                // handle both String and List formats
                                List<String> allowedTypes;
                                if (fileTypeObj instanceof List) {
                                    allowedTypes = (List<String>) fileTypeObj;
                                } else {

                                    allowedTypes = Arrays.stream(fileTypeObj.toString().split(","))
                                            .map(ext -> ext.trim().startsWith(".") ? ext.trim() : "." + ext.trim())
                                            .collect(Collectors.toList());
                                }
                                boolean validExtension = allowedTypes.stream()
                                        .anyMatch(ext -> valueStr.toLowerCase().endsWith(ext.toLowerCase()));
                                if (!validExtension) {
                                    throw new RuntimeException(
                                            "Field '" + label + "' must be one of these file types: " + allowedTypes);
                                }
                            }

                            // check maxSize in KB using actual uploaded file
                            Object maxSizeObj = validations.get("maxSize");
                            if (maxSizeObj != null) {
                                long maxSizeKB = ((Number) maxSizeObj).longValue();

                                // find actual uploaded file by matching filename

                                if (uploadedFile != null) {
                                    // getSize() returns bytes — divide by 1024 for KB
                                    long fileSizeKB = uploadedFile.getSize() / 1024;
                                    if (fileSizeKB > maxSizeKB) {
                                        throw new RuntimeException(
                                                "Field '" + label +
                                                        "' file size must not exceed " + maxSizeKB +
                                                        " KB. Uploaded file is " + fileSizeKB + " KB");
                                    }
                                }
                            }

                            break;
                    }
                }
            }
        }
    }

    public List<FormResponseDTO> getResponses(UUID formId) {
        return repository.findByFormId(formId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<FormResponseDTO> getByEmail(String email) {
        return repository.findByEmail(email)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private FormResponseDTO mapToDTO(FormResponse entity) {
        FormResponseDTO dto = new FormResponseDTO();
        dto.setResponseId(entity.getResponseId());
        /*Form form = new Form();
        form.setId(dto.getFormId());
        entity.setForm(form);*/
        dto.setFormId(entity.getForm().getId());
        dto.setResponse(entity.getResponse());
        dto.setSubmittedAt(entity.getSubmittedAt());
        return dto;
    }

    private FormResponse mapToEntity(FormResponseDTO dto) {
        FormResponse entity = new FormResponse();
        entity.setResponseId(dto.getResponseId());
        // dto.setFormId(entity.getForm().getId());
        Form form = formRepository.findById(dto.getFormId())
                .orElseThrow(() -> new RuntimeException("Form not found with id: " + dto.getFormId()));
        entity.setForm(form);
        entity.setResponse(dto.getResponse());
        entity.setSubmittedAt(dto.getSubmittedAt());
        return entity;
    }
}

