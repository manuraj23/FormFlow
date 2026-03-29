package com.FormFlow.FormFlow.Controller.User;

import com.FormFlow.FormFlow.DTO.FormDetails.FormCreateDTO;
import com.FormFlow.FormFlow.DTO.FormDetails.FormGetDTO;
import com.FormFlow.FormFlow.Service.User.UserService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Operation(summary = "Create a new form")
    @PostMapping("/createForm")
    public ResponseEntity<?> createForm(@RequestBody FormCreateDTO dto) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            userService.createForm(dto, username);
            return new ResponseEntity<>("Form Created Successfully", HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>("Error Creating Form", HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Get all forms")
    @GetMapping("/allForm")
    public ResponseEntity<List<FormGetDTO>> getAllForms() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        List<FormGetDTO> forms = userService.getAllForms(username);

        if (forms != null && !forms.isEmpty()) {
            return new ResponseEntity<>(forms, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @Operation(summary = "Get forms by their published status (true for published, false for draft)")
    @GetMapping("/status/{status}")
    public List<FormGetDTO> getFormsByStatus(@PathVariable String status) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userService.getFormsByStatus(username,status);
    }


//    @Operation(summary = "Update a form by ID")
//    @PutMapping("/form/{id}")
//    public ResponseEntity<?> updateForm(@PathVariable Long id,@RequestBody FormCreateDTO dto) {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        String username = authentication.getName();
//        userService.updateForm(id, dto, username);
//        return new ResponseEntity<>("Form Updated Successfully", HttpStatus.OK);
//    }

}