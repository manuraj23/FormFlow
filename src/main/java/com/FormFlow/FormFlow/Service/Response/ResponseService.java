package com.FormFlow.FormFlow.Service.Response;

import com.FormFlow.FormFlow.DTO.Response.FormResponseDTO;
import com.FormFlow.FormFlow.Entity.*;
import com.FormFlow.FormFlow.Repository.*;
import com.FormFlow.FormFlow.enums.RoleType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ResponseService {

    private final FormResponseRepository repository;
    private final FormSectionRepository formSectionRepository;
    private final FormRepository formRepository;
    private final UserRepository userRepository;
    private final UserFormRoleRepository userFormRoleRepository;
    private final ConditionalLogicService conditionalLogicService;
    private final FormTimerRepository formTimerRepository;
    private final FormTempTimerRepository formTempTimerRepository;

    // reads upload directory from application-dev.yml
    @Value("${file.upload-dir}")
    private String uploadDir;

    public ResponseService(FormResponseRepository repository,
                           FormSectionRepository formSectionRepository,
                           FormRepository formRepository,
                           UserRepository userRepository,
                           UserFormRoleRepository userFormRoleRepository,
                           ConditionalLogicService conditionalLogicService,
                           FormTimerRepository formTimerRepository,
                           FormTempTimerRepository formTempTimerRepository
    ) {
        this.repository = repository;
        this.formSectionRepository = formSectionRepository;
        this.formRepository = formRepository;
        this.userRepository=userRepository;
        this.userFormRoleRepository=userFormRoleRepository;
        this.conditionalLogicService=conditionalLogicService;
        this.formTimerRepository = formTimerRepository;
        this.formTempTimerRepository = formTempTimerRepository;
    }

    // updated to accept files alongside response data
    public FormResponseDTO saveResponse(FormResponseDTO dto,
                                        List<MultipartFile> files, String username, UUID tempUserId) {


        Map<String, String> uploadedFileMap = new HashMap<>();

        //validate form limits
        validateFormLimits(dto.getFormId(), username);

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

        Map<String, Object> evaluation = null;
        Double score = null;
        Double totalScore = null;

        // check if quiz
        Form form = formRepository.findById(dto.getFormId()).orElseThrow();


//        User user = null;
//        if (username != null && !username.equals("anonymousUser")) {
//            user = userRepository.findByUsername(username);
//        }


        Map<String, Object> settings = form.getSettings();

      /*  boolean isQuiz = Boolean.TRUE.equals(settings.get("isQuizMode"))
                || Boolean.TRUE.equals(settings.get("isQuiz"));*/


        boolean isQuiz = settings != null && (
                Boolean.TRUE.equals(settings.get("isQuizMode"))
                        || Boolean.TRUE.equals(settings.get("isQuiz"))
        );
        if(isQuiz){
            validateQuizTimer(dto.getFormId(), username, tempUserId);
        }
        if (isQuiz) {
            evaluation = evaluateQuiz(dto.getFormId(), responseMap);
            score = ((Number) evaluation.get("score")).doubleValue();
//            totalScore = ((Number) evaluation.get("maxScore")).doubleValue();
//            evaluation.put("TotalScore", totalScore);
        }

        // Save response to DB
        FormResponse entity = mapToEntity(dto);
        if (username != null) {
            User user = userRepository.findByUsername(username);
            if (user == null) {
                throw new RuntimeException("User not found");
            }
            entity.setUser(user);
        }
        entity.setSubmittedAt(LocalDateTime.now());
        entity.setScore(score);
        entity.setEvaluation(evaluation);

        if (settings != null) {
            Object editWindowObj = settings.get("editWindowMinutes");
            if (editWindowObj != null) {
                int editWindowMinutes = ((Number) editWindowObj).intValue();
                entity.setEditableUntil(
                        Instant.now().plusSeconds(editWindowMinutes * 60L)
                );
            }
        }

        // if username present, fetch user and link to response
        if (username != null) {
            User user = userRepository.findByUsername(username);
            if (user == null) {
                throw new RuntimeException("User not found");
            }
            entity.setUser(user);
        }

        FormResponse saved = repository.save(entity);
        boolean showScore = settings != null &&
                Boolean.parseBoolean(String.valueOf(settings.get("showScore")));

        FormResponseDTO responseDTO = mapToDTO(saved);

        if (!showScore) {
            responseDTO.setScore(null);
            responseDTO.setEvaluation(null);
        }

        return responseDTO;
    }

    public FormResponseDTO editResponse(UUID responseId,
                                        FormResponseDTO dto,
                                        List<MultipartFile> files,
                                        String username) {

        //  fetch existing response
        FormResponse existing = repository.findById(responseId)
                .orElseThrow(() -> new RuntimeException("Response not found"));

        //  block edits on quiz forms
        Form form = existing.getForm();
        Map<String, Object> settings = form.getSettings();

        boolean isQuiz = settings != null && (
                Boolean.TRUE.equals(settings.get("isQuizMode"))
                        || Boolean.TRUE.equals(settings.get("isQuiz"))
        );

        if (isQuiz) {
            throw new RuntimeException(
                    "Quiz responses cannot be edited after submission");
        }

        //anonymous responses cannot be edited
        if (existing.getUser() == null) {
            throw new RuntimeException(
                    "This response was submitted anonymously and cannot be edited");
        }

        // only the user who submitted can edit
        if (!existing.getUser().getUsername().equals(username)) {
            throw new RuntimeException(
                    "You are not authorized to edit this response");
        }

        // edit window check
        if (existing.getEditableUntil() == null) {
            throw new RuntimeException(
                    "Editing is not allowed for this form");
        }
        if (Instant.now().isAfter(existing.getEditableUntil())) {
            throw new RuntimeException(
                    "Edit window has closed. Responses could be edited until "
                            + existing.getEditableUntil());
        }

        // re-run full validation on the new data — same as submission
        validateResponse(existing.getForm().getId(), dto.getResponse(), files);

        //  handle file uploads
        Map<String, String> uploadedFileMap = new HashMap<>();
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String savedFileName = saveFileToLocal(file);
                    uploadedFileMap.put(file.getOriginalFilename(), savedFileName);
                }
            }
        }

        //  update response map with file URLs
        Map<String, Object> responseMap = dto.getResponse();
        for (Map.Entry<String, Object> entry : new HashMap<>(responseMap).entrySet()) {
            String fieldId = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String) {
                String fileName = (String) value;
                if (uploadedFileMap.containsKey(fileName)) {
                    String savedFileName = uploadedFileMap.get(fileName);
                    String fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                            .path("/uploads/")
                            .path(savedFileName)
                            .toUriString();
                    responseMap.put(fieldId, fileUrl);
                }
            }
        }
        //  overwrite response and track edit time
        existing.setResponse(responseMap);
        existing.setLastEditedAt(Instant.now());
       /*
        FormResponse saved = repository.save(existing);
        return mapToDTO(saved);*/
      //  System.out.println("lastEditedAt before save: " + existing.getLastEditedAt());
        repository.saveAndFlush(existing);

// re-fetch to get the actual persisted state
        FormResponse saved = repository.findById(existing.getResponseId())
                .orElseThrow(() -> new RuntimeException("Response not found"));

    //    System.out.println("lastEditedAt after fetch: " + saved.getLastEditedAt());

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

    private void validateFormLimits(UUID formId, String username) {

        Form form = formRepository.findById(formId)
                .orElseThrow(() -> new RuntimeException("Form not found"));

        Map<String, Object> settings = form.getSettings();
        if (settings == null) return;

        // isPrivate check — rejecting if form is private and user is not logged in
        Object isPrivate = settings.get("isPrivate");
        if (Boolean.TRUE.equals(isPrivate)) {
            if (username == null) {
                throw new RuntimeException(
                        "This form is private. Please log in to submit a response");
            }

            //  checking if this private form has any assigned responders
            List<UserFormRole> assignedRoles = userFormRoleRepository.findByFormId(formId);

            boolean hasAssignedResponders = assignedRoles.stream()
                    .anyMatch(r -> r.getRole() == RoleType.RESPONDER);

            if (hasAssignedResponders) {
                // form has assigned responders — only they can submit
                boolean isAssignedResponder = assignedRoles.stream()
                        .anyMatch(r -> r.getRole() == RoleType.RESPONDER
                                && r.getUser().getUsername().equals(username));

                if (!isAssignedResponder) {
                    throw new RuntimeException("This form is only accessible to assigned responders");
                }
            }
            // if no assigned responders — any logged in user can submit
            // a user can submit a private form only once
            boolean alreadyResponded = repository.existsByForm_IdAndUser_Username(formId, username);

            if (alreadyResponded) {
                throw new RuntimeException("You have already submitted a response for this form");
            }
        }

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

            long currentCount = repository.countByForm_Id(formId);

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
        // build fieldId -> FormFields map for recursive chain resolution
        Map<String, FormFields> fieldMap = sections.stream()
                .flatMap(s -> s.getFields().stream())
                .filter(f -> f.getId() != null)
                .collect(Collectors.toMap(
                        f -> f.getId().toString(),
                        f -> f
                ));
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

                String fieldId = field.getId() != null ? field.getId().toString() : null;

                //  skip field if its condition makes it inactive
                /*Object fieldLogic = field.getFieldLogic();
                boolean fieldActive = conditionalLogicService.isActive(fieldLogic, submittedResponse);*/
                boolean fieldActive = conditionalLogicService.isActive(
                        field.getFieldLogic(),
                        submittedResponse,
                        fieldMap
                );
                if (!fieldActive) {
                    if (fieldId != null) {
                        submittedResponse.remove(fieldId);
                    }
                    continue;
                }


                Map<String, Object> config = field.getFieldConfig();
                if (config == null) continue;

                String label = (String) config.get("label");
                if (label == null) continue;

                Map<String, Object> validations =
                        (Map<String, Object>) config.get("validations");

                List<String> options = (List<String>) config.get("options");

                // changed: looking up submitted value by field ID instead of label ***
                // String fieldId = field.getId() != null ? field.getId().toString() : null;
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
                          /*  if (options != null && !options.isEmpty()) {
                                if (!options.contains(valueStr)) {
                                    throw new RuntimeException(
                                            "Field '" + label + "' must be one of: " + options);
                                }
                            }*/
                            List<String> trimmedOptions1 = options.stream()
                                    .map(String::trim)
                                    .collect(Collectors.toList());
                            if (!trimmedOptions1.contains(valueStr.trim())) {
                                throw new RuntimeException(
                                        "Field '" + label + "' must be one of: " + options);
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
                             /*   for (String s : selected) {
                                    if (!options.contains(s.trim())) {
                                        throw new RuntimeException(
                                                "Field '" + label + "' contains invalid option: " + s.trim());
                                    }
                                }*/

                                List<String> trimmedOptions = options.stream()
                                        .map(String::trim)
                                        .collect(Collectors.toList());

                                for (String s : selected) {
                                    if (!trimmedOptions.contains(s.trim())) {
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
        return repository.findByForm_Id(formId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public Map<String, Boolean> hasUserResponded(UUID formId, String username) {

        boolean hasResponded = repository.existsByForm_IdAndUser_Username(formId, username);
        return Map.of("hasResponded", hasResponded);
    }

    public List<FormResponseDTO> getByEmail(String email) {
        return repository.findByUser_Email(email)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    public Map<String, Long> getUniqueAssignees(UUID formId) {
        long count = userFormRoleRepository.countDistinctAssigneesByFormIdAndRole(formId, RoleType.RESPONDER);
        return Map.of("count", count);
    }

    public Map<String, Long> getUniqueRespondents(UUID formId) {
        long count = repository.countDistinctRespondentsByFormId(formId);
        return Map.of("count", count);
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
        dto.setScore(entity.getScore());
        dto.setEvaluation(entity.getEvaluation());
       dto.setEditableUntil(entity.getEditableUntil());
        dto.setLastEditedAt(entity.getLastEditedAt());

        // return username so frontend knows who submitted
        if (entity.getUser() != null) {
            dto.setUsername(entity.getUser().getUsername());
        }
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
        // user is set separately after this function is called
    }


    private Map<String, Object> evaluateQuiz(UUID formId, Map<String, Object> responseMap) {

        Map<String, Object> evaluation = new HashMap<>();
        double totalScore = 0;
        double maxScore = 0;

        List<FormSection> sections =
                formSectionRepository.findByFormIdInWithFields(List.of(formId));

        for (FormSection section : sections) {

            for (FormFields field : section.getFields()) {

                Map<String, Object> quizConfig = field.getQuizConfig();
                if (quizConfig == null || quizConfig.isEmpty()) {
                    continue;
                }

                if (!Boolean.TRUE.equals(quizConfig.get("isScored"))) {
                    continue;
                }

                String fieldId = field.getId().toString();
                String label = (String) field.getFieldConfig().get("label");

                Object rawAnswer = responseMap.get(fieldId);

                if (rawAnswer == null && label != null) {
                    rawAnswer = responseMap.get(label);
                }

                if (rawAnswer == null) {
                    continue;
                }

                List<String> userAnswers;

                if (rawAnswer instanceof List) {
                    userAnswers = ((List<?>) rawAnswer).stream()
                            .map(String::valueOf)
                            .map(String::trim)
                            .toList();
                } else {
                    userAnswers = List.of(rawAnswer.toString().trim());
                }

                Object correctAnswer = quizConfig.get("correctAnswer");

                double points = quizConfig.get("points") == null
                        ? 0.0
                        : ((Number) quizConfig.get("points")).doubleValue();

                double negativeMarks = quizConfig.get("negativeMarks") == null
                        ? 0.0
                        : ((Number) quizConfig.get("negativeMarks")).doubleValue();

                boolean isCorrect;

                if (correctAnswer instanceof List) {

                    Set<String> correctSet = ((List<?>) correctAnswer).stream()
                            .map(String::valueOf)
                            .map(String::trim)
                            .collect(java.util.stream.Collectors.toSet());

                    isCorrect = new HashSet<>(userAnswers).equals(correctSet);
                }

                else {
                    String correct = String.valueOf(correctAnswer).trim();

                    isCorrect = userAnswers.size() == 1 &&
                            userAnswers.get(0).equalsIgnoreCase(correct);
                }
                maxScore += points;

                if (isCorrect) {
                    totalScore += points;
                } else {
                    totalScore -= negativeMarks;
                }
            }
        }

        evaluation.put("score", totalScore);
        evaluation.put("maxScore", maxScore);
        return evaluation;
    }

    public void startTimer(UUID formId, String username, UUID tempUserId) {
        if ((username == null  || username.isBlank()) && tempUserId == null) {
            throw new RuntimeException("User identity missing");
        }
        if(tempUserId != null){
            startPublicTimer(formId, tempUserId);
            return;
        }
//        if(username == null){
//            throw new RuntimeException("User not authenticated");
//        }
        User user = userRepository.findByUsername(username);
        if(user == null){
            throw new RuntimeException("User not found");
        }
        Form form = formRepository.findById(formId).orElseThrow(() -> new RuntimeException("Form not found"));

        Map<String, Object> settings = form.getSettings();
        if (settings == null || settings.get("duration") == null) {
            throw new RuntimeException("Duration not configured in form settings");
        }
        Object durationObj = settings.get("duration");
        if(!(durationObj instanceof Number number)){
            throw new RuntimeException("Invalid duration format");
        }

        int duration = number.intValue();
        Optional<FormTimer> existing = formTimerRepository.findByForm_IdAndUser_UserId(formId, user.getUserId());
        FormTimer timer;
        if (existing.isPresent()) {
            // UPDATE EXISTING TIMER
            timer = existing.get();
            timer.setStartTime(LocalDateTime.now());
            timer.setDuration(duration);
        } else {
            // CREATE NEW TIMER
            timer = new FormTimer();
            timer.setUser(user);
            timer.setForm(form);
            timer.setStartTime(LocalDateTime.now());
            timer.setDuration(duration);
        }
        formTimerRepository.save(timer);
    }

    public void startPublicTimer(UUID formId, UUID tempUserId) {
        if(tempUserId == null){
            throw new RuntimeException("Backend Error : plz connect with backend team");
        }
        Form form = formRepository.findById(formId)
                .orElseThrow(() -> new RuntimeException("Form not found"));
        Map<String, Object> settings = form.getSettings();
        if (settings == null || settings.get("duration") == null) {
            throw new RuntimeException("Duration not configured in form settings");
        }
        Object durationObj = settings.get("duration");
        if(!(durationObj instanceof Number number)){
            throw new RuntimeException("Invalid duration format");
        }
        int duration = number.intValue();
        Optional<FormTempTimer> existing = formTempTimerRepository.findByForm_IdAndTempUserId(formId, tempUserId);
        FormTempTimer timer;
        if (existing.isPresent()) {
            // UPDATE EXISTING TIMER
            timer = existing.get();
            timer.setStartTime(LocalDateTime.now());
            timer.setDuration(duration);
        } else {
            // CREATE NEW TIMER
            timer = new FormTempTimer();
            timer.setTempUserId(tempUserId);
            timer.setForm(form);
            timer.setStartTime(LocalDateTime.now());
            timer.setDuration(duration);
        }
        formTempTimerRepository.save(timer);
    }


    private void validateQuizTimer(UUID formId, String username, UUID tempUserId) {
        //public user
        if (tempUserId != null) {
            Optional<FormTempTimer> optionalTimer =
                    formTempTimerRepository.findTopByForm_IdAndTempUserIdOrderByStartTimeDesc(formId, tempUserId);
            if (optionalTimer.isEmpty()){
                return;
            }
            FormTempTimer timer = optionalTimer.get();
            if (timer.getDuration() == null || timer.getStartTime() == null){
                return;
            }
            LocalDateTime now = LocalDateTime.now();
            long minutesSpent = java.time.Duration.between(timer.getStartTime(), now).toMinutes() - 1;
            if (minutesSpent > timer.getDuration()) {
                throw new RuntimeException("Quiz duration exceeded");
            }
            return;
        }

        // CASE 2: AUTH USER

        if (username == null) {
            return;
        }
        User user = userRepository.findByUsername(username);
        if (user == null){
            return;
        }
        Optional<FormTimer> optionalTimer = formTimerRepository.findTopByForm_IdAndUser_UserIdOrderByStartTimeDesc(
                formId, user.getUserId());
        if (optionalTimer.isEmpty()) {
            return;
        }
        FormTimer timer = optionalTimer.get();
        if (timer.getDuration() == null || timer.getStartTime() == null){
            return;
        }
        LocalDateTime serverNow = LocalDateTime.now();
        long minutesSpent = java.time.Duration.between(timer.getStartTime(), serverNow).toMinutes() - 1;

        if (minutesSpent > timer.getDuration()) {
            throw new RuntimeException("Quiz duration exceeded");
        }
    }
}
