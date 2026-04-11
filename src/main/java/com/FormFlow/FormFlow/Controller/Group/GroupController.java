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

    @Operation(summary = "Add members to Group")
    @PostMapping("/{groupId}/addMembers")
    public ResponseEntity<?> addMembersToGroup(@PathVariable UUID groupId, @RequestBody List<String> members) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            groupService.addMembers(groupId, members, username);
            return ResponseEntity.ok(Map.of(
                    "message", "Members added successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error adding members: " + e.getMessage());
        }
    }

    @Operation(summary = "Promote users to Admin")
    @PostMapping("/{groupId}/addAdmins")
    public ResponseEntity<?> addAdmins(@PathVariable UUID groupId, @RequestBody List<String> usernames) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = authentication.getName();
            groupService.addAdmins(groupId, usernames, currentUsername);
            return ResponseEntity.ok(Map.of(
                    "message", "Admins added successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error adding admins: " + e.getMessage());
        }
    }

    @Operation(summary = "Demote admins to members")
    @PostMapping("/{groupId}/removeAdmins")
    public ResponseEntity<?> removeAdmins(@PathVariable UUID groupId, @RequestBody List<String> usernames) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = authentication.getName();
            groupService.removeAdmins(groupId, usernames, currentUsername);
            return ResponseEntity.ok(Map.of(
                    "message", "Admins demoted to members successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error removing admins: " + e.getMessage());
        }
    }

    @Operation(summary = "Invite members using invite Link")
    @PostMapping("/{groupId}/invite")
    public ResponseEntity<?> generateInvite(@PathVariable UUID groupId, @RequestParam int minutesValid) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        String link = groupService.generateInviteLink(groupId, username, minutesValid);
        return ResponseEntity.ok(Map.of(
                "message", "Invite link generated",
                "link", link
        ));
    }

    @Operation(summary = "Join Group using invite link")
    @PostMapping("/joinByInviteCode")
    public ResponseEntity<?> joinGroup(@RequestParam String token) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body("User must be logged in");
        }
        String username = auth.getName();
        groupService.joinGroupByInvite(token, username);
        return ResponseEntity.ok(Map.of(
                "message", "Joined group successfully"
        ));
    }

    @Operation(summary = "Remove members/admins from group")
    @PostMapping("/{groupId}/removeUsers")
    public ResponseEntity<?> removeUsers(@PathVariable UUID groupId, @RequestBody List<String> usernames) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = auth.getName();
            groupService.removeUsers(groupId, usernames, currentUsername);
            return ResponseEntity.ok(Map.of(
                    "message", "Users removed successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error removing users: " + e.getMessage());
        }
    }

}
