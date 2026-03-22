package com.FormFlow.FormFlow.Controller;

import com.FormFlow.FormFlow.DTO.FormGetDTO;
import com.FormFlow.FormFlow.Service.FormService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
// Base path — all endpoints here start with /forms
// combined with context path /formflow it becomes /formflow/forms
@RequestMapping("/forms")
@CrossOrigin(origins = "*")
public class FormController {

    private final FormService formService;

    public FormController(FormService formService) {
        this.formService = formService;
    }

    // GET /formflow/forms/{id}
    // Returns one specific form by its ID as a FormGetDTO
    @GetMapping("/{id}")
    public FormGetDTO getFormById(@PathVariable Long id) {
        return formService.getFormById(id);
    }

    // GET /formflow/forms/status/{status}
    // status will be "true" for published or "false" for draft

    @GetMapping("/status/{status}")
    public List<FormGetDTO> getFormsByStatus(@PathVariable String status) {
        boolean published = status.equalsIgnoreCase("true");
        return formService.getFormsByStatus(published);
    }
}