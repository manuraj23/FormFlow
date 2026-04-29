package com.FormFlow.FormFlow.Config;

import com.FormFlow.FormFlow.DTO.Auth.Oauth.OAuth2ResponseDTO;
import com.FormFlow.FormFlow.Service.Authentication.OAuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final OAuthService oAuthService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = token.getPrincipal();

        String registrationId = token.getAuthorizedClientRegistrationId();

        // 🔹 Generate tokens (your existing logic)
        OAuth2ResponseDTO authResponse =
                oAuthService.handleOAuth2Request(oAuth2User, registrationId);

        // 🔹 STEP 1: get state (page where user came from)
        String state = request.getParameter("state");

        String redirectPath = "/home"; // default

        if (state != null && !state.isEmpty()) {
            try {
                redirectPath = new String(Base64.getDecoder().decode(state));
            } catch (Exception e) {
                redirectPath = "/home";
            }
        }

        // 🔹 STEP 2: build final redirect URL
        String baseUrl = frontendUrl.endsWith("/")
                ? frontendUrl.substring(0, frontendUrl.length() - 1)
                : frontendUrl;

        String redirectUrl = baseUrl + "/oauth-success" +
                "?accessToken=" + URLEncoder.encode(authResponse.getAccessToken(), StandardCharsets.UTF_8) +
                "&refreshToken=" + URLEncoder.encode(authResponse.getRefreshToken(), StandardCharsets.UTF_8) +
                "&redirect=" + URLEncoder.encode(redirectPath, StandardCharsets.UTF_8);

        // 🔹 STEP 3: redirect
        response.sendRedirect(redirectUrl);
    }
}