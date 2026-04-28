package com.FormFlow.FormFlow.Controller.Response;

import com.FormFlow.FormFlow.DTO.Response.FormResponseDTO;
import com.FormFlow.FormFlow.Service.Response.ResponseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/responses")
@CrossOrigin
public class ResponseController {

    private final ResponseService service;

    public ResponseController(ResponseService service) {
        this.service = service;
    }
    /*
    // JSON endpoint — no files, standard request body
    @Operation(summary = "Submit a response without files")
    @PostMapping
    public FormResponseDTO submit(@RequestBody FormResponseDTO responseDTO) {
        return service.saveResponse(responseDTO, null);
    } */

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public FormResponseDTO submit(@RequestBody FormResponseDTO responseDTO,
                                  @RequestParam(required = false) UUID tempUserId)
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return service.saveResponse(responseDTO, null, username, tempUserId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FormResponseDTO submitWithFiles(
            @RequestPart("response") String responseJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(required = false) UUID tempUserId
    ) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            FormResponseDTO responseDTO = mapper.readValue(responseJson, FormResponseDTO.class);
            // extract currently logged in username from JWT token
            // will be null if no token provided - (anonymous user)
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username= null;
            if( auth !=null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")){
                username=auth.getName();
            }

            return service.saveResponse(responseDTO, files, username, tempUserId);

        } catch (Exception e) {
            throw new RuntimeException("Invalid JSON format", e);
        }
    }

    @PutMapping(value = "/{responseId}/edit",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FormResponseDTO editResponse(
            @PathVariable UUID responseId,
            @RequestPart("response") String responseJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()
                || auth.getPrincipal().equals("anonymousUser")) {
            throw new RuntimeException(
                    "You must be logged in to edit a response");
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            FormResponseDTO dto = mapper.readValue(responseJson, FormResponseDTO.class);
            String username = auth.getName();
            return service.editResponse(responseId, dto, files, username);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Invalid JSON format", e);
        }
    }

    @Operation(summary = "Get all responses for a specific form by its ID")
    @GetMapping("/{formId}")
    public List<FormResponseDTO> getResponses(@PathVariable UUID formId) {
        return service.getResponses(formId);
    }

    @Operation(summary = "Get all responses submitted by a specific email")
    @GetMapping("/email/{email}")
    public List<FormResponseDTO> getByEmail(@PathVariable String email) {
        return service.getByEmail(email);
    }

    @GetMapping("/assignees/{formId}")
    public Map<String, Long> getUniqueAssignees(@PathVariable UUID formId) {
        return service.getUniqueAssignees(formId);
    }

    @GetMapping("/respondents/{formId}")
    public Map<String, Long> getUniqueRespondents(@PathVariable UUID formId) {
        return service.getUniqueRespondents(formId);
    }
    @PostMapping("/timerStart/{formId}")
    public Map<String,String> startTimer(@PathVariable UUID formId,  @RequestParam(required = false) UUID tempUserId) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String username = authentication.getName();
        service.startTimer(formId, username, tempUserId);
        return Map.of("message","Timer started successfully");
    }

    @GetMapping("/hasResponded/{formId}")
    public Map<String, Boolean> hasUserResponded(@PathVariable UUID formId) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()
                || auth.getPrincipal().equals("anonymousUser")) {
            throw new RuntimeException("User not authenticated");
        }

        String username = auth.getName();

        return service.hasUserResponded(formId, username);
    }

}
