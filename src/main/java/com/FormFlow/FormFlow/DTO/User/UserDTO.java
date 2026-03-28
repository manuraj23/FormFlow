package com.FormFlow.FormFlow.DTO.User;
import lombok.Data;
import java.util.List;

@Data
public class UserDTO {
    private Long userId;
    private String username;
    private String email;
    private List<String> roles;
}
