package com.FormFlow.FormFlow.Controller.Admin;

import com.FormFlow.FormFlow.DTO.FormDetails.FormGetDTO;
import com.FormFlow.FormFlow.DTO.User.UserDTO;
import com.FormFlow.FormFlow.Service.Admin.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Operation(summary = "Get All Users")
    @GetMapping("/getAllUsers")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = adminService.getAllUsers();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @Operation(summary = "Get All Forms")
    @GetMapping("/getAllForms")
    public ResponseEntity<List<FormGetDTO>> getAllForms() {
        List<FormGetDTO> forms = adminService.getAllForms();
        return new ResponseEntity<>(forms, HttpStatus.OK);
    }



}
