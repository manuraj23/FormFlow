package com.FormFlow.FormFlow.DTO.Auth.Oauth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OAuth2ResponseDTO {
    private String accessToken;
    private String refreshToken;
}
