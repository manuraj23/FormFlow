package com.FormFlow.FormFlow.Controller.Form;

import com.FormFlow.FormFlow.DTO.FormDetails.FormCreateDTO;
import com.FormFlow.FormFlow.DTO.FormDetails.FormGetDTO;
import com.FormFlow.FormFlow.Service.Form.CreateFormService;
import com.FormFlow.FormFlow.Service.Form.GetFormService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CreateGetFormController {

    private final CreateFormService createFormService;

    private final GetFormService getFormService;


    @Operation(summary = "Create a new form")
    @PostMapping("/createForm")
    public String createForm(@RequestBody FormCreateDTO dto) {
        return createFormService.createForm(dto);
    }

    @Operation(summary = "Get all forms")
    @GetMapping("/allForm")
    public List<FormGetDTO> getAllForms() {
        return getFormService.getAllForms();
    }
}
