package com.FormFlow.FormFlow.DTO.Auth.ForgetPassword;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class ForgotPasswordDTO {
    @JsonAlias({"username", "email"})
    private String identifier;
}
