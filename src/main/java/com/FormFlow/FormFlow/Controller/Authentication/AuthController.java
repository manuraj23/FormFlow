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
import org.springframework.mail.MailException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

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

    //Check username and Email exist in DB or not

    @Operation(summary = "Check if username already exists")
    @PostMapping("/usernameCheck")
    public ResponseEntity<?> usernameCheck(@RequestBody String userName) {
        boolean exists = authService.usernameCheck(userName);
        return ResponseEntity.ok(Map.of("available", !exists));
    }

    @Operation(summary = "Check if email already exists")
    @PostMapping("/emailCheck")
    public ResponseEntity<?> emailCheck(@RequestBody String email) {
        boolean exists = authService.emailCheck(email);
        return ResponseEntity.ok(Map.of("available", !exists));
    }

    // SignUp Section

    @Operation(summary = "Signup Method")
    @PostMapping("/signup")
    public ResponseEntity<?>newSignUP(@RequestBody SignUpDTOnew signUpDTOnew){
        try{
            authService.saveNewUserNew(signUpDTOnew);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "OTP sent to " + signUpDTOnew.getEmail() + ", Verify your account"));

        }catch(IllegalStateException ex){
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Username already exists");
        } catch (IllegalArgumentException ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ex.getMessage());
        }
    }

    @Operation(summary = "Verify Account using OTP")
    @PostMapping("/verifyAccount")
    public ResponseEntity<?>verifyAccount(@RequestBody VarifyAccountDTO varifyAccountDTO){
        try{
            AuthResponseDTO response =authService.verifyAccount(varifyAccountDTO);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException ex){
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("OTP expired or invalid");
        } catch (IllegalArgumentException ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ex.getMessage());
        }

    }

    @Operation(summary = "Resend OTP for account verification")
    @PostMapping("/resendOtpVerifyaccount")
    public ResponseEntity<?> resendOtp(@RequestBody String email) {
        authService.resendOtpVerifyAccount(email);
        return ResponseEntity.ok(Map.of(
                "message", "OTP resent successfully to " + email,
                "email", email
        ));
    }


    //Login Section

    @Operation(summary = "Login using Username or Email")
    @PostMapping("/login")
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


    //Logout Section

    @Operation(summary = "Logout a user by invalidating their refresh token")
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody LogoutDTO logoutDTO) {

        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(logoutDTO.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        refreshTokenRepository.delete(refreshToken);

        return ResponseEntity.ok("Logged out successfully");
    }


    //Forget-Password Section
    @Operation(summary = "Forget Password - Send OTP to registered email")
    @PostMapping("/forgotPassword")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordDTO dto){
        try {
            String email=authService.forgotPassword(dto.getIdentifier());
            return ResponseEntity.ok(Map.of("email", email));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        }
    }

    @Operation(summary = "Resend OTP for password reset")
    @PostMapping("/resendOtpResetPassword")
    public ResponseEntity<?> resendOtpResetPassword(@RequestBody String email) {
        authService.resendOtpResetPassword(email);
        return ResponseEntity.ok(Map.of(
                "message", "OTP resent successfully to " + email,
                "email", email
        ));
    }

    @Operation(summary = "Verify OTP for password reset")
    @PostMapping("/verifyResetOtp")
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
    @PostMapping("/resetPassword")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordDTO dto){
        try {
            AuthResponseDTO response = authService.resetPassword(dto.getEmail(), dto.getNewPassword());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        }
    }

    @ExceptionHandler(MailException.class)
    public ResponseEntity<?> handleMailException(MailException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                Map.of(
                        "message", "Unable to send OTP email right now. Please try again shortly.",
                        "error", "MAIL_SERVICE_UNAVAILABLE"
                )
        );
    }

}