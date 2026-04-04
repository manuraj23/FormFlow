package com.FormFlow.FormFlow.Controller.Authentication;
import com.FormFlow.FormFlow.DTO.Auth.*;
import com.FormFlow.FormFlow.DTO.Auth.ForgetPassword.ForgotPasswordDTO;
import com.FormFlow.FormFlow.DTO.Auth.ForgetPassword.ResetPasswordDTO;
import com.FormFlow.FormFlow.DTO.Auth.ForgetPassword.VerifyResetOtpDTO;
import com.FormFlow.FormFlow.DTO.Auth.Login.NewLoginDTO;
import com.FormFlow.FormFlow.DTO.Auth.SignUp.SignUpDTOnew;
import com.FormFlow.FormFlow.DTO.Auth.SignUp.VarifyAccountDTO;
import com.FormFlow.FormFlow.Entity.RefreshToken;
import com.FormFlow.FormFlow.Entity.User;
import com.FormFlow.FormFlow.Repository.RefreshTokenRepository;
import com.FormFlow.FormFlow.Service.Authentication.AuthService;
import com.FormFlow.FormFlow.Service.Authentication.RefreshTokenService;
import com.FormFlow.FormFlow.Utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtils jwtUtil;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

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

    @Operation(summary = "New Signup Method")
    @PostMapping("/newsignup")
    public ResponseEntity<?>newSignUP(@RequestBody SignUpDTOnew signUpDTOnew){
        try{
            authService.saveNewUserNew(signUpDTOnew);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "OTP sent to email, Verify your account"));

        }catch(IllegalStateException ex){
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Username already exists");
        } catch (IllegalArgumentException ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ex.getMessage());
        }
    }

    @Operation(summary = "Varify Account using OTP")
    @PostMapping("/varifyaccount")
    public ResponseEntity<?>varifyAccount(@RequestBody VarifyAccountDTO varifyAccountDTO){
        try{
            authService.verifyAccount(varifyAccountDTO);
            return ResponseEntity.ok("Account Verified Successfully");
        } catch (IllegalStateException ex){
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("OTP expired or invalid");
        } catch (IllegalArgumentException ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ex.getMessage());
        }

    }

    @Operation(summary = "Login a registered user")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO loginDTO) {
        return ResponseEntity.ok(authService.login(loginDTO));
    }

    @Operation(summary = "Login using Username or Email")
    @PostMapping("/loginnew")
    public ResponseEntity<?> loginNew(@RequestBody NewLoginDTO newLoginDTO) {
        try {
            return ResponseEntity.ok(authService.loginNew(newLoginDTO));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid username/email or password");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ex.getMessage());
        }
    }

    @Operation(summary = "Refresh access token using a valid refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequestDTO request) {

        RefreshToken refreshToken = refreshTokenService
                .verifyExpiration(
                        refreshTokenRepository.findByToken(request.getRefreshToken())
                                .orElseThrow(() -> new RuntimeException("Invalid refresh token"))
                );

        User user = refreshToken.getUser();
        String accessToken = jwtUtil.generateToken(user.getUsername(), user.getRoles());

        return ResponseEntity.ok(new AuthResponseDTO(accessToken, refreshToken.getToken()));
    }

    @Operation(summary = "Logout a user by invalidating their refresh token")
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody LogoutDTO logoutDTO) {

        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(logoutDTO.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        refreshTokenRepository.delete(refreshToken);

        return ResponseEntity.ok("Logged out successfully");
    }

    @Operation(summary = "Forget Password - Send OTP to registered email")
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordDTO dto){
        try {
            authService.forgotPassword(dto.getIdentifier());
            return ResponseEntity.ok("OTP sent to registered email");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        }
    }

    @Operation(summary = "Verify OTP for password reset")
    @PostMapping("/verify-reset-otp")
    public ResponseEntity<?> verifyResetOtp(@RequestBody VerifyResetOtpDTO dto){
        try {
            authService.verifyResetOtp(dto.getEmail(), dto.getOtp());
            return ResponseEntity.ok("OTP verified");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        }
    }

    @Operation(summary = "Reset Password entering new password")
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordDTO dto){
        try {
            authService.resetPassword(dto.getEmail(), dto.getNewPassword());
            return ResponseEntity.ok("Password reset successful");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        }
    }

}