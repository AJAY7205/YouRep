package com.learning.ytrep.controller;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.learning.ytrep.config.Idempotent;
import com.learning.ytrep.model.AppRole;
import com.learning.ytrep.model.Role;
import com.learning.ytrep.model.User;
import com.learning.ytrep.repository.RoleRepository;
import com.learning.ytrep.repository.UserRepository;
import com.learning.ytrep.security.jwt.JwtUtils;
import com.learning.ytrep.security.jwt.TokenBlacklistService;
import com.learning.ytrep.security.request.LoginRequest;
import com.learning.ytrep.security.request.SendVerificationCodeRequest;
import com.learning.ytrep.security.request.SignupRequest;
import com.learning.ytrep.security.request.VerifyEmailRequest;
import com.learning.ytrep.service.EmailService;
import com.learning.ytrep.service.VerificationCodeService;
import com.learning.ytrep.security.response.JwtResponse;
import com.learning.ytrep.security.response.MessageResponse;
import com.learning.ytrep.security.services.UserDetailsImpl;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;
    private final TokenBlacklistService tokenBlacklistService;
    private final VerificationCodeService verificationCodeService;
    private final EmailService emailService;

    private static final long LOCK_DURATION_MINUTES = 15;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder encoder,
            JwtUtils jwtUtils,
            TokenBlacklistService tokenBlacklistService,
            VerificationCodeService verificationCodeService,
            EmailService emailService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
        this.tokenBlacklistService = tokenBlacklistService;
        this.verificationCodeService = verificationCodeService;
        this.emailService = emailService;
    }
    @Operation(summary = "User Login")
    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElse(null);

        if (user != null && user.isAccountLocked()) {
            if (user.getLockTime() != null &&
                    user.getLockTime().plusMinutes(LOCK_DURATION_MINUTES).isBefore(LocalDateTime.now())) {
                user.setAccountLocked(false);
                user.setFailedLoginAttempts(0);
                user.setLockTime(null);
                userRepository.save(user);
            } else {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Account is locked due to multiple failed login attempts"));
            }
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(item -> item.getAuthority())
                    .collect(Collectors.toList());

            if (user != null) {
                user.setFailedLoginAttempts(0);
                user.setAccountLocked(false);
                user.setLockTime(null);
                user.setLastLogin(LocalDateTime.now());
                userRepository.save(user);
            }

            return ResponseEntity.ok(new JwtResponse(jwt,
                    userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getEmail(),
                    roles,
                    user != null && user.isEmailVerified()));

        } catch (Exception e) {
            if (user != null) {
                user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
                if (user.getFailedLoginAttempts() >= 5) {
                    user.setAccountLocked(true);
                    user.setLockTime(LocalDateTime.now());
                }
                userRepository.save(user);
            }
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Invalid username or password"));
        }
    }
    @Operation(summary = "User Registration")
    @Idempotent
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        // Create new user
        User user = new User(signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()));

        Set<String> strRoles = signUpRequest.getRoles();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null || strRoles.isEmpty()) {
            // Default role is USER
            Role userRole = roleRepository.findByAppRole(AppRole.USER)
                    .orElseGet(() -> {
                        Role newRole = new Role(AppRole.USER);
                        return roleRepository.save(newRole);
                    });
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role.toLowerCase()) {
                    case "admin":
                        // Admin signup is not allowed via API
                        throw new RuntimeException("Error: Admin role cannot be assigned via signup!");
                    case "guest":
                        Role guestRole = roleRepository.findByAppRole(AppRole.GUEST)
                                .orElseGet(() -> {
                                    Role newRole = new Role(AppRole.GUEST);
                                    return roleRepository.save(newRole);
                                });
                        roles.add(guestRole);
                        break;
                    default:
                        Role userRole = roleRepository.findByAppRole(AppRole.USER)
                                .orElseGet(() -> {
                                    Role newRole = new Role(AppRole.USER);
                                    return roleRepository.save(newRole);
                                });
                        roles.add(userRole);
                }
            });
        }

        user.setRoles(roles);
        userRepository.save(user);

        // Send the verification code (non-blocking: never fail signup if email delivery fails)
        try {
            String code = verificationCodeService.generateAndStoreCode(user.getEmail());
            emailService.sendVerificationCode(user.getEmail(), code);
        } catch (Exception e) {
            log.error("Failed to send verification code for {}: {}", user.getEmail(), e.getMessage());
        }

        // Auto-login after successful signup
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), signUpRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        List<String> userRoles = user.getRoles().stream()
                .map(role -> role.getAppRole().name())
                .collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(jwt,
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                userRoles,
                user.isEmailVerified()));
    }

    @Operation(summary = "Send 6-digit verification code to email")
    @PostMapping("/send-verification-code")
    public ResponseEntity<?> sendVerificationCode(@Valid @RequestBody SendVerificationCodeRequest request,
                                                  Authentication authentication) {
        boolean owner = isOwner(authentication, request.getEmail());
        if (authentication != null && !owner) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("You can only request a code for your own email"));
        }
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElse(null);
        if (user == null) {
            if (owner) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("No account found for this email"));
            }
            return ResponseEntity.ok(new MessageResponse("If an account exists for this email, a verification code has been sent."));
        }
        if (user.isEmailVerified()) {
            if (owner) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Email is already verified"));
            }
            return ResponseEntity.ok(new MessageResponse("If an account exists for this email, a verification code has been sent."));
        }
        String code = verificationCodeService.generateAndStoreCode(request.getEmail());
        try {
            emailService.sendVerificationCode(request.getEmail(), code);
        } catch (Exception e) {
            verificationCodeService.invalidate(request.getEmail());
            throw e;
        }
        if (owner) {
            return ResponseEntity.ok(new MessageResponse("Verification code sent successfully"));
        }
        return ResponseEntity.ok(new MessageResponse("If an account exists for this email, a verification code has been sent."));
    }

    @Operation(summary = "Verify email with 6-digit code")
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody VerifyEmailRequest request,
                                         Authentication authentication) {
        if (authentication != null && !isOwner(authentication, request.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("You can only verify your own email"));
        }
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("No account found for this email"));
        }
        if (user.isEmailVerified()) {
            return ResponseEntity.ok(new MessageResponse("Email is already verified"));
        }
        verificationCodeService.verify(request.getEmail(), request.getCode());
        user.setEmailVerified(true);
        userRepository.save(user);
        return ResponseEntity.ok(new MessageResponse("Email verified successfully"));
    }

    private boolean isOwner(Authentication authentication, String email) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return false;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetailsImpl userDetails) {
            return email != null && email.equalsIgnoreCase(userDetails.getEmail());
        }
        return false;
    }

    @Operation(summary = "User Logout")
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            if (jwtUtils.validateJwtToken(jwt)) {
                String jti = jwtUtils.getJtiFromJwtToken(jwt);
                tokenBlacklistService.blacklistToken(jti, jwtUtils.getExpirationMs());
            }
        }
        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }
}
