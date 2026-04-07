package com.FormFlow.FormFlow.Service.Authentication;
import com.FormFlow.FormFlow.DTO.Auth.AuthResponseDTO;
import com.FormFlow.FormFlow.DTO.Auth.Login.NewLoginDTO;
import com.FormFlow.FormFlow.DTO.Auth.SignUp.SignUpDTOnew;
import com.FormFlow.FormFlow.DTO.Auth.SignUp.VarifyAccountDTO;
import com.FormFlow.FormFlow.Entity.RefreshToken;
import com.FormFlow.FormFlow.Entity.User;
import com.FormFlow.FormFlow.Entity.UserEntity.PasswordResetTemp;
import com.FormFlow.FormFlow.Entity.UserEntity.TempUser;
import com.FormFlow.FormFlow.Repository.User.PasswordResetTempRepository;
import com.FormFlow.FormFlow.Repository.User.TempUserRepository;
import com.FormFlow.FormFlow.Repository.UserRepository;
import com.FormFlow.FormFlow.Service.Email.ResetPasswordEmailService;
import com.FormFlow.FormFlow.Service.Email.VarifyAccountEmailService;
import com.FormFlow.FormFlow.Utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class AuthService {
    @Autowired
    public UserRepository userRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtil;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private TempUserRepository tempUserRepository;

    @Autowired
    private VarifyAccountEmailService varifyAccountEmailService;

    @Autowired
    private PasswordResetTempRepository passwordResetTempRepository;

    @Autowired
    private ResetPasswordEmailService resetPasswordEmailService;

    private static final PasswordEncoder passwordEncoder= new BCryptPasswordEncoder();

    public boolean usernameCheck(String userName) {
        return userRepository.existsByUsername(userName);
    }

    public boolean emailCheck(String email) {
        return userRepository.existsByEmail(email);
    }


    //Login Section
    public AuthResponseDTO loginNew(NewLoginDTO newLoginDTO) {
        String identifier = normalizeIdentifier(newLoginDTO.getIdentifier());
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Username or email is required");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        identifier,
                        newLoginDTO.getPassword()
                )
        );

        User user = userRepository
                .findByUsernameOrEmail(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String accessToken = jwtUtil.generateToken(user.getUsername(), user.getRoles());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        return new AuthResponseDTO(accessToken, refreshToken.getToken());
    }

    private String normalizeIdentifier(String identifier) {
        if (identifier == null) {
            return null;
        }

        String trimmed = identifier.trim();
        if (trimmed.contains("@")) {
            return trimmed.toLowerCase();
        }

        return trimmed;
    }


    //Signup Section

    @Transactional
    public void saveNewUserNew(SignUpDTOnew signUpDTOnew) {
        if(userRepository.existsByUsername(signUpDTOnew.getUsername()))
            throw new IllegalStateException("Username already exists");

        if(userRepository.existsByEmail(signUpDTOnew.getEmail()))
            throw new IllegalStateException("Email already exists");

        if(tempUserRepository.existsByEmail(signUpDTOnew.getEmail())){
            TempUser tempUser=tempUserRepository.findByEmail(signUpDTOnew.getEmail()).orElseThrow();
            tempUser.setEmail(signUpDTOnew.getEmail());
            tempUser.setUsername(signUpDTOnew.getUsername());
            tempUser.setPassword(passwordEncoder.encode(signUpDTOnew.getPassword()));
            String otp = String.valueOf(new Random().nextInt(900000) + 100000);
            tempUser.setOtp(otp);
            tempUser.setOtpAttempts(0);
            tempUser.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
            tempUser.setCreatedAt(LocalDateTime.now());
            tempUserRepository.save(tempUser);
            varifyAccountEmailService.sendOtp(signUpDTOnew.getEmail(), otp);
            return;
        }

        String otp = String.valueOf(new Random().nextInt(900000) + 100000);

        TempUser tempUser = new TempUser();
        tempUser.setEmail(signUpDTOnew.getEmail());
        tempUser.setUsername(signUpDTOnew.getUsername());
        tempUser.setPassword(passwordEncoder.encode(signUpDTOnew.getPassword()));
        tempUser.setOtp(otp);
        tempUser.setOtpAttempts(0);
        tempUser.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        tempUser.setCreatedAt(LocalDateTime.now());
        tempUserRepository.save(tempUser);

        varifyAccountEmailService.sendOtp(signUpDTOnew.getEmail(), otp);
    }

//    @Transactional
//    public void saveNewUserNew(SignUpDTOnew signUpDTOnew) {
//
//        if(userRepository.existsByUsername(signUpDTOnew.getUsername()))
//            throw new IllegalStateException("Username already exists");
//
//        if(userRepository.existsByEmail(signUpDTOnew.getEmail()))
//            throw new IllegalStateException("Email already exists");
//
//        Optional<TempUser> existing = tempUserRepository.findByEmail(signUpDTOnew.getEmail());
//
//        String otp = String.valueOf(new Random().nextInt(900000) + 100000);
//
//        if(existing.isPresent()){
//            TempUser tempUser = existing.get();
//
//            // 🔒 Rate limiting
//            if(tempUser.getCreatedAt() != null &&
//                    tempUser.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(30))) {
//                throw new IllegalStateException("Please wait 30 seconds before requesting another OTP");
//            }
//
//            // ✅ Check expiry BEFORE updating
//            boolean expired = tempUser.getOtpExpiry() != null &&
//                    tempUser.getOtpExpiry().isBefore(LocalDateTime.now());
//
//            if(expired) {
//                tempUser.setOtpAttempts(0);
//            }
//
//            tempUser.setEmail(signUpDTOnew.getEmail());
//            tempUser.setUsername(signUpDTOnew.getUsername());
//            tempUser.setPassword(passwordEncoder.encode(signUpDTOnew.getPassword()));
//            tempUser.setOtp(otp);
//            tempUser.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
//            tempUser.setCreatedAt(LocalDateTime.now());
//
//            tempUserRepository.save(tempUser);
//            varifyAccountEmailService.sendOtp(signUpDTOnew.getEmail(), otp);
//            return;
//        }
//
//        TempUser tempUser = new TempUser();
//        tempUser.setEmail(signUpDTOnew.getEmail());
//        tempUser.setUsername(signUpDTOnew.getUsername());
//        tempUser.setPassword(passwordEncoder.encode(signUpDTOnew.getPassword()));
//        tempUser.setOtp(otp);
//        tempUser.setOtpAttempts(0);
//        tempUser.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
//        tempUser.setCreatedAt(LocalDateTime.now());
//        tempUserRepository.save(tempUser);
//        varifyAccountEmailService.sendOtp(signUpDTOnew.getEmail(), otp);
//    }

    public void resendOtpVerifyAccount(String email) {
        TempUser tempUser = tempUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if(tempUser.getOtpAttempts() >= 5){
            tempUserRepository.deleteByEmail(email);
            throw new IllegalStateException("Max OTP attempts reached. Sign up again.");
        }
        String otp = String.valueOf(new Random().nextInt(900000) + 100000);
        tempUser.setOtp(otp);
        tempUser.setOtpAttempts(0);
        tempUser.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        tempUserRepository.save(tempUser);
        varifyAccountEmailService.sendOtp(email, otp);
    }

    @Transactional
    public AuthResponseDTO verifyAccount(VarifyAccountDTO varifyAccountDTO) {
        TempUser tempUser = tempUserRepository.findByEmail(varifyAccountDTO.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if(tempUser.getOtpAttempts() >= 5){
            tempUserRepository.deleteByEmail(varifyAccountDTO.getEmail());
            throw new IllegalStateException("Max OTP attempts reached");
        }

        if(tempUser.getOtpExpiry().isBefore(LocalDateTime.now())){
            tempUserRepository.deleteByEmail(varifyAccountDTO.getEmail());
            throw new IllegalStateException("OTP expired");
        }

        if(!tempUser.getOtp().equals(varifyAccountDTO.getOtp())){
            tempUser.setOtpAttempts(tempUser.getOtpAttempts() + 1);
            tempUserRepository.save(tempUser);
            throw new IllegalArgumentException("Invalid OTP");
        }

        User user = new User();
        user.setEmail(tempUser.getEmail());
        user.setUsername(tempUser.getUsername());
        user.setPassword(tempUser.getPassword());
        user.setRoles(List.of("USER"));
        userRepository.save(user);
        tempUserRepository.deleteByEmail(varifyAccountDTO.getEmail());
        String accessToken = jwtUtil.generateToken(user.getUsername(), user.getRoles());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        return new AuthResponseDTO(accessToken, refreshToken.getToken());
    }


    //Forget-Password Section
    public String forgotPassword(String identifier) {
        String normalizedIdentifier = normalizeIdentifier(identifier);
        if (normalizedIdentifier == null || normalizedIdentifier.isBlank()) {
            throw new IllegalArgumentException("Username or email is required");
        }

        User user = userRepository
                .findByUsernameOrEmail(normalizedIdentifier, normalizedIdentifier)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String email = user.getEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("No email is linked to this account");
        }

        String otp = String.valueOf(new Random().nextInt(900000) + 100000);

        if(passwordResetTempRepository.existsByEmail(email)){
            PasswordResetTemp existingUser=passwordResetTempRepository.findByEmail(email).orElseThrow();
            existingUser.setOtp(otp);
            existingUser.setOtpAttempts(0);
            existingUser.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
            existingUser.setVerified(false);
            passwordResetTempRepository.save(existingUser);
            resetPasswordEmailService.sendOtp(email, otp);
            return user.getEmail();
        }

        PasswordResetTemp temp = new PasswordResetTemp();
        temp.setEmail(email);
        temp.setOtp(otp);
        temp.setOtpAttempts(0);
        temp.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        temp.setVerified(false);
        passwordResetTempRepository.save(temp);
        resetPasswordEmailService.sendOtp(email, otp);
        return user.getEmail();
    }

    @Transactional
    public void verifyResetOtp(String email, String otp) {

        PasswordResetTemp temp = passwordResetTempRepository
                .findByEmail(email)
                .orElseThrow(() -> new RuntimeException("OTP not requested"));

        if (temp.isVerified()) {
            throw new IllegalStateException("OTP already verified. Proceed to reset password.");
        }

        if(temp.getOtpAttempts() >= 5){
            passwordResetTempRepository.deleteByEmail(email);
            throw new IllegalStateException("Max OTP attempts reached. Move to Forget password page");
        }

        if(temp.getOtpExpiry().isBefore(LocalDateTime.now())){
            passwordResetTempRepository.deleteByEmail(email);
            throw new IllegalStateException("OTP expired. Move to Forget password page");
        }

        if(!temp.getOtp().equals(otp)){
            temp.setOtpAttempts(temp.getOtpAttempts() + 1);
            passwordResetTempRepository.save(temp);
            throw new IllegalArgumentException("Invalid OTP");
        }

        temp.setVerified(true);
        passwordResetTempRepository.save(temp);
    }

    @Transactional
    public AuthResponseDTO resetPassword(String email, String newPassword) {

        PasswordResetTemp temp = passwordResetTempRepository
                .findByEmail(email)
                .orElseThrow(() -> new RuntimeException("OTP not verified"));

        if (!temp.isVerified()) {
            throw new IllegalStateException("OTP verification required before resetting password");
        }
        if(temp.getOtpExpiry().isBefore(LocalDateTime.now())){
            passwordResetTempRepository.deleteByEmail(email);
            throw new IllegalStateException("Reset request expired. Request OTP again.");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        passwordResetTempRepository.deleteByEmail(email);

        String accessToken = jwtUtil.generateToken(user.getUsername(), user.getRoles());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        return new AuthResponseDTO(accessToken, refreshToken.getToken());
    }

    public void resendOtpResetPassword(String email) {
        PasswordResetTemp temp = passwordResetTempRepository
                .findByEmail(email)
                .orElseThrow(() -> new RuntimeException("OTP not requested"));

        if(temp.getOtpAttempts() >= 5){
            passwordResetTempRepository.deleteByEmail(email);
            throw new IllegalStateException("Max OTP attempts reached. Move to Forget password page");
        }

        String otp = String.valueOf(new Random().nextInt(900000) + 100000);
        temp.setOtp(otp);
        temp.setOtpAttempts(0);
        temp.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        temp.setVerified(false);
        passwordResetTempRepository.save(temp);
        resetPasswordEmailService.sendOtp(email, otp);
    }
}
