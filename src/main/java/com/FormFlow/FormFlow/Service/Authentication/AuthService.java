package com.FormFlow.FormFlow.Service.Authentication;
import com.FormFlow.FormFlow.DTO.Auth.SignUpDTO;
import com.FormFlow.FormFlow.Entity.User;
import com.FormFlow.FormFlow.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AuthService {
    @Autowired
    public UserRepository userRepository;

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


}

