package com.FormFlow.FormFlow.DTO.Auth.ForgetPassword;

import lombok.Data;

@Data
public class VerifyResetOtpDTO {
    private String email;
    private String otp;
}
