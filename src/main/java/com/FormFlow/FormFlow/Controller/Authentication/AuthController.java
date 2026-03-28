package com.FormFlow.FormFlow.Controller.Authentication;

import com.FormFlow.FormFlow.DTO.Auth.LoginDTO;
import com.FormFlow.FormFlow.DTO.Auth.SignUpDTO;
import com.FormFlow.FormFlow.Service.Authentication.AuthService;
import com.FormFlow.FormFlow.Service.UserDetailServiceImpl;
import com.FormFlow.FormFlow.Utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailServiceImpl userDetailService;

    @Autowired
    private JwtUtils jwtUtil;

    @Operation(summary = "Register a new user")
    @PostMapping("/signup")
    public ResponseEntity<?>signup(@RequestBody SignUpDTO signUpDTO){
        try{
            authService.saveNewUser(signUpDTO);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "User registered successfully"));
        } catch(IllegalStateException ex){
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Username already exists");
        } catch (IllegalArgumentException ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ex.getMessage());
        }
    }

    @Operation(summary = "Login and receive a JWT token")
    @PostMapping("login")
    public ResponseEntity<?>login(@RequestBody LoginDTO loginDTO) {
        try {
            String username = loginDTO.getUsername();
            String password = loginDTO.getPassword();
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
            UserDetails userDetails = userDetailService.loadUserByUsername(username);
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority).filter(Objects::nonNull)
                    .map(authority -> authority.startsWith("ROLE_") ? authority.substring(5) : authority)
                    .toList();
            String jwt = jwtUtil.generateToken(userDetails.getUsername(), roles);
            return new ResponseEntity<>(jwt, HttpStatus.OK);
        } catch (BadCredentialsException e) {
            return new ResponseEntity<>("Invalid username or password", HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            return new ResponseEntity<>("Invalid username or password", HttpStatus.UNAUTHORIZED);
        }


    }


}