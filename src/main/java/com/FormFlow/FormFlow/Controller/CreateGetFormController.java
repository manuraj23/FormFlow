package com.FormFlow.FormFlow.Controller;

import com.FormFlow.FormFlow.DTO.FormCreateDTO;
import com.FormFlow.FormFlow.DTO.FormGetDTO;
import com.FormFlow.FormFlow.Service.CreateFormService;
import com.FormFlow.FormFlow.Service.GetFormService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CreateGetFormController {

    private final CreateFormService createFormService;
    private final GetFormService getFormService;

    @PostMapping("/createForm")
    public String createForm(@RequestBody FormCreateDTO dto) {
        return createFormService.createForm(dto);
    }

    @GetMapping("/allForm")
    public List<FormGetDTO> getAllForms() {
        return getFormService.getAllForms();
    }
}