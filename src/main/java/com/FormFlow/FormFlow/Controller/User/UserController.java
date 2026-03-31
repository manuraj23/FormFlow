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

    @Operation(summary = "Get a form by its ID")
    @GetMapping("/form/{id}")
    public FormGetDTO getFormById(@PathVariable Long id){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userService.getFormById(username,id);
    }

    @Operation(summary = "Move to Trash by form id")
    @PatchMapping("/form/moveToTrash/{id}")
    public ResponseEntity<?> softDeleteForm(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        userService.softDeleteForm(username, id);
        return ResponseEntity.ok("Form Moved to Trash successfully");
    }

    @Operation(summary = "Get All Forms in Trash")
    @GetMapping("/form/trash")
    public ResponseEntity<List<FormGetDTO>> getTrashedForms() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        List<FormGetDTO> trashedForms = userService.getTrashedForms(username);
        if (trashedForms != null && !trashedForms.isEmpty()) {
            return new ResponseEntity<>(trashedForms, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @Operation(summary = "Restore deleted form from Trash by form id")
    @PatchMapping("/form/restoreFromTrash/{id}")
    public ResponseEntity<?> restoreDeletedForm(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        userService.restoreDeletedForm(username, id);
        return ResponseEntity.ok("Form Recovered from Trash successfully");
    }

    @Operation(summary = "Update a form by ID (if no responses exist)")
    @PutMapping("/updateForm/{id}")
    public ResponseEntity<?> updateForm(@PathVariable Long id, @RequestBody FormCreateDTO dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        try {
            boolean updated = userService.updateForm(id, dto, username);
            if (updated) {
                return new ResponseEntity<>("Form Updated Successfully", HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Version control is remaining", HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            return new ResponseEntity<>("Error Updating Form: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

}