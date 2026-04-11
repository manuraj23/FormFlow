package com.FormFlow.FormFlow.Entity.OAuthProviders;

import com.FormFlow.FormFlow.Entity.User;
import com.FormFlow.FormFlow.enums.OAuthProviderType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "oauth_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuthAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String providerId;

    @Enumerated(EnumType.STRING)
    private OAuthProviderType providerType;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
