package com.FormFlow.FormFlow.Controller;

import com.FormFlow.FormFlow.DTO.Response.FormResponseDTO;
import com.FormFlow.FormFlow.Service.ResponseService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/responses")
@CrossOrigin
public class ResponseController {

    private final ResponseService service;

    public ResponseController(ResponseService service) {
        this.service = service;
    }

    @Operation(summary = "Submit a response to a form")
    @PostMapping
    public FormResponseDTO submit(@RequestBody FormResponseDTO response) {
        return service.saveResponse(response);
    }

    @Operation(summary = "Get all responses for a specific form by its ID")
    @GetMapping("/{formId}")
    public List<FormResponseDTO> getResponses(@PathVariable Long formId) {
        return service.getResponses(formId);
    }

    @Operation(summary = "Get all responses submitted by a specific email")
    @GetMapping("/email/{email}")
    public List<FormResponseDTO> getByEmail(@PathVariable String email) {
        return service.getByEmail(email);
    }
}
