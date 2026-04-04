package com.FormFlow.FormFlow.DTO.Auth.ForgetPassword;

import lombok.Data;

@Data
public class ResetPasswordDTO {
    private String email;
    private String newPassword;
}
