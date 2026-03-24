package com.FormFlow.FormFlow.Controller;

import com.FormFlow.FormFlow.DTO.FormResponseDTO;
import com.FormFlow.FormFlow.Service.ResponseService;
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

    @PostMapping
    public FormResponseDTO submit(@RequestBody FormResponseDTO response) {
        return service.saveResponse(response);
    }

    @GetMapping("/{formId}")
    public List<FormResponseDTO> getResponses(@PathVariable Long formId) {
        return service.getResponses(formId);
    }

    @GetMapping("/email/{email}")
    public List<FormResponseDTO> getByEmail(@PathVariable String email) {
        return service.getByEmail(email);
    }
}
