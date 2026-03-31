package com.FormFlow.FormFlow.Controller.Public;


import com.FormFlow.FormFlow.DTO.FormDetails.FormGetDTO;
import com.FormFlow.FormFlow.Service.Public.PublicService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public")
public class PublicController {
    
    @Autowired
    private PublicService publicService;
    
    @Operation(summary = "Get a form by its ID")
    @GetMapping("/form/{id}")
    public FormGetDTO getFormById(@PathVariable Long id){
        return publicService.getFormById(id);
    }
}
