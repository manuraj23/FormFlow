package com.FormFlow.FormFlow.Service.Authentication;
import com.FormFlow.FormFlow.DTO.Auth.AuthResponseDTO;
import com.FormFlow.FormFlow.DTO.Auth.LoginDTO;
import com.FormFlow.FormFlow.DTO.Auth.SignUpDTO;
import com.FormFlow.FormFlow.Entity.RefreshToken;
import com.FormFlow.FormFlow.Entity.User;
import com.FormFlow.FormFlow.Repository.UserRepository;
import com.FormFlow.FormFlow.Utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AuthService {
    @Autowired
    public UserRepository userRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtil;

    private static final PasswordEncoder passwordEncoder= new BCryptPasswordEncoder();

    public void saveNewUser(SignUpDTO signUpDTO) {
        if (signUpDTO.getUsername() == null || signUpDTO.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (signUpDTO.getPassword() == null || signUpDTO.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (userRepository.existsByUsername(signUpDTO.getUsername())) {
            throw new IllegalStateException("Username already exists");
        }

        User user = new User();
        user.setUsername(signUpDTO.getUsername());
        user.setPassword(passwordEncoder.encode(signUpDTO.getPassword()));
        user.setRoles(List.of("USER"));

        userRepository.save(user);
    }

    @Autowired
    private RefreshTokenService refreshTokenService;

    public AuthResponseDTO login(LoginDTO loginDTO) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDTO.getUsername(),
                        loginDTO.getPassword()
                )
        );

        User user = userRepository.findByUsername(loginDTO.getUsername());

        String accessToken = jwtUtil.generateToken(user.getUsername(), user.getRoles());

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return new AuthResponseDTO(accessToken, refreshToken.getToken());
    }


}

