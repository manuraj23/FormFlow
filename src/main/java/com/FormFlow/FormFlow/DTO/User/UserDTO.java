package com.FormFlow.FormFlow.DTO.User;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class UserDTO {
    private UUID userId;
    private String username;
    private String email;
    private List<String> roles;
}
