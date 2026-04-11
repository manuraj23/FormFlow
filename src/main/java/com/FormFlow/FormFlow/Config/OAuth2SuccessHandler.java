package com.FormFlow.FormFlow.Config;

import com.FormFlow.FormFlow.DTO.Auth.Oauth.OAuth2ResponseDTO;
import com.FormFlow.FormFlow.Service.Authentication.OAuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final OAuthService oAuthService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = token.getPrincipal();

        String registrationId = token.getAuthorizedClientRegistrationId();

        OAuth2ResponseDTO authResponse = oAuthService.handleOAuth2Request(oAuth2User, registrationId);

        String redirectUrl = "http://localhost:4200/oauth-success" +
                "?accessToken=" + authResponse.getAccessToken() +
                "&refreshToken=" + authResponse.getRefreshToken();
        response.sendRedirect(redirectUrl);
    }
}
