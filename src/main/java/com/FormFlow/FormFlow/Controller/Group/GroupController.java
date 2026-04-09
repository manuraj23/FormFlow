package com.FormFlow.FormFlow.Controller.Group;

import com.FormFlow.FormFlow.DTO.Group.GroupCreateDTO;
import com.FormFlow.FormFlow.DTO.Group.GroupUser;
import com.FormFlow.FormFlow.Entity.GroupEntity.Group;
import com.FormFlow.FormFlow.Service.Group.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/group")
public class GroupController {
    @Autowired
    private GroupService groupService;

    @Operation(summary = "Create a new Group")
    @PostMapping("/createGroup")
    public ResponseEntity<?> createForm(@RequestBody GroupCreateDTO groupCreateDTO) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            Group group = groupService.createGroup(groupCreateDTO, username);
            return ResponseEntity.ok().body(Map.of(
                    "message", "Group created successfully",
                    "group", group));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error Creating Group: " + e.getMessage());
        }
    }

    @Operation(summary = "Get all created groups by a user")
    @GetMapping("/myGroups")
    public ResponseEntity<?> getMyGroups() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            List<Group> groups = groupService.getGroupsOfUser(username);
            return ResponseEntity.ok(Map.of(
                    "message", "Groups fetched successfully",
                    "groups", groups));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching groups: " + e.getMessage());
        }
    }

    @Operation(summary = "Get all admins of a particular group")
    @GetMapping("/{groupId}/admins")
    public ResponseEntity<?> getGroupAdmins(@PathVariable UUID groupId) {
        try {
            List<GroupUser> admins = groupService.getGroupAdmins(groupId);
            return ResponseEntity.ok(Map.of(
                    "message", "Group admins fetched successfully",
                    "admins", admins));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching group admins: " + e.getMessage());
        }
    }

    @Operation(summary = "Get all members of a particular group")
    @GetMapping("/{groupId}/members")
    public ResponseEntity<?> getGroupMembers(@PathVariable UUID groupId) {
        try {
            List<GroupUser> members = groupService.getGroupMembers(groupId);
            return ResponseEntity.ok(Map.of(
                    "message", "Group members fetched successfully",
                    "members", members));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching group members: " + e.getMessage());
        }
    }

}
