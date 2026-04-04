package com.FormFlow.FormFlow.DTO.Auth.Login;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class NewLoginDTO {
    @JsonAlias({"username", "email"})
    private String identifier;
    private String password;
}
