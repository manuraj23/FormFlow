package com.FormFlow.FormFlow.Service.Authentication;
import com.FormFlow.FormFlow.DTO.Auth.Oauth.OAuth2ResponseDTO;
import com.FormFlow.FormFlow.Entity.OAuthProviders.OAuthAccount;
import com.FormFlow.FormFlow.Entity.RefreshToken;
import com.FormFlow.FormFlow.Entity.User;
import com.FormFlow.FormFlow.Repository.OauthRepo.OAuthAccountRepository;
import com.FormFlow.FormFlow.Repository.UserRepository;
import com.FormFlow.FormFlow.Utils.JwtUtils;
import com.FormFlow.FormFlow.enums.OAuthProviderType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OAuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private OAuthAccountRepository oAuthAccountRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Transactional
    public OAuth2ResponseDTO handleOAuth2Request(OAuth2User oAuth2User, String registrationId) {
        OAuthProviderType providerType = getProviderType(registrationId);
        String providerId = getProviderId(oAuth2User, registrationId);
        String emailFromOauth = oAuth2User.getAttribute("email");
        if (emailFromOauth == null) throw new IllegalArgumentException("Email not found from provider");

        OAuthAccount existingAccount = oAuthAccountRepository.findByProviderTypeAndProviderId(providerType, providerId).orElse(null);

        User user;
        if (existingAccount != null) {
            user = existingAccount.getUser();
        } else {
            user = userRepository.findByEmail(emailFromOauth).orElse(null);
            if (user == null) {
                String username = getUsername(emailFromOauth);
                user = new User();
                user.setUsername(username);
                user.setEmail(emailFromOauth);
                user.setRoles(List.of("USER"));
                userRepository.save(user);
            }
            OAuthAccount newAccount = new OAuthAccount();
            newAccount.setProviderType(providerType);
            newAccount.setProviderId(providerId);
            newAccount.setUser(user);

            oAuthAccountRepository.save(newAccount);
        }
        String accessToken = jwtUtils.generateToken(user.getUsername(), user.getRoles());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return new OAuth2ResponseDTO(accessToken, refreshToken.getToken());
    }

    private OAuthProviderType getProviderType(String registrationId){
        return switch( registrationId.toLowerCase()){
            case "google"->OAuthProviderType.GOOGLE;
            case "github"->OAuthProviderType.GITHUB;
            default -> throw new IllegalArgumentException("Unsupported provider: " + registrationId);
        };
    }

    private String getProviderId(OAuth2User user, String registrationId){
        return switch (registrationId.toLowerCase()){
            case "google"-> user.getAttribute("sub");
            case "github"-> String.valueOf(user.getAttribute("id"));
            default -> throw new IllegalArgumentException("Unsupported provider: " + registrationId);
        };
    }

    private String getUsername(String email){
        String username=email.split("@")[0];
        int count=1;
        String base=username;
        while(userRepository.existsByUsername(username)){
            username=base+count++;
        }
        return username;
    }
}
