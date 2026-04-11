package com.FormFlow.FormFlow.Repository.OauthRepo;

import com.FormFlow.FormFlow.Entity.OAuthProviders.OAuthAccount;
import com.FormFlow.FormFlow.enums.OAuthProviderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, UUID> {
    Optional<OAuthAccount> findByProviderTypeAndProviderId(OAuthProviderType providerType, String providerId);
}
