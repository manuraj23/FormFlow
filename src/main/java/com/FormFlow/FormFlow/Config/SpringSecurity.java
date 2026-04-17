package com.FormFlow.FormFlow.Config;
import com.FormFlow.FormFlow.Filters.JwtFilter;
import com.FormFlow.FormFlow.Service.UserDetailServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SpringSecurity {

    private final JwtFilter jwtFilter;
    private final UserDetailServiceImpl userDetailsService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(allMatchers(
                                "/auth/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/public/**",
                                "/uploads/**",
                                "/oauth2/**",
                                "/health",
                                "/error"
                        )).permitAll()

                        .requestMatchers(allMatchers("/user/**", "/group/**","/api/**","/ai/**")).authenticated()
                        .requestMatchers(allMatchers("/admin/**")).hasRole("ADMIN")

                        .anyRequest().denyAll()
                )
                .oauth2Login(oauth -> oauth
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler((req, res, ex) -> {
                            String baseUrl = frontendUrl.endsWith("/")
                                    ? frontendUrl.substring(0, frontendUrl.length() - 1)
                                    : frontendUrl;

                            res.sendRedirect(baseUrl + "/login?error=true");
                        })
                );

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider =
                new DaoAuthenticationProvider(userDetailsService);

        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private String[] withContextPath(String pattern) {
        String normalizedContextPath = (contextPath == null) ? "" : contextPath.trim();
        if (normalizedContextPath.isEmpty() || "/".equals(normalizedContextPath)) {
            return new String[]{pattern};
        }

        if (!normalizedContextPath.startsWith("/")) {
            normalizedContextPath = "/" + normalizedContextPath;
        }
        if (normalizedContextPath.endsWith("/")) {
            normalizedContextPath = normalizedContextPath.substring(0, normalizedContextPath.length() - 1);
        }

        String withoutPrefix = pattern;
        String withPrefix = normalizedContextPath + pattern;
        return new String[]{withoutPrefix, withPrefix};
    }

    private String[] allMatchers(String... patterns) {
        List<String> all = new ArrayList<>();
        for (String pattern : patterns) {
            for (String resolved : withContextPath(pattern)) {
                all.add(resolved);
            }
        }
        return all.toArray(new String[0]);
    }
}
