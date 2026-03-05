package com.jobportal.controller;

import com.jobportal.dto.JwtResponse;
import com.jobportal.dto.LoginRequest;
import com.jobportal.dto.SignupRequest;
import com.jobportal.entity.Role;
import com.jobportal.entity.User;
import com.jobportal.repository.UserRepository;
import com.jobportal.security.CustomUserDetails;
import com.jobportal.security.JwtUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String role = userDetails.getAuthorities().iterator().next().getAuthority();

            return ResponseEntity.ok(new JwtResponse(jwt,
                    userDetails.getId(),
                    userDetails.getName(),
                    userDetails.getUsername(),
                    role));
        } catch (org.springframework.security.core.AuthenticationException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Invalid email or password!");
            return ResponseEntity.status(401).body(response);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Error: Email is already in use!");
            return ResponseEntity.badRequest().body(response);
        }

        if (!signUpRequest.getEmail().toLowerCase().endsWith("@gmail.com")) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Error: Only @gmail.com addresses are allowed for registration!");
            return ResponseEntity.badRequest().body(response);
        }

        Role userRole = signUpRequest.getRole() != null ? signUpRequest.getRole() : Role.ROLE_JOB_SEEKER;

        User user = new User(
                signUpRequest.getName(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()),
                userRole);

        if (userRole == Role.ROLE_RECRUITER) {
            user.setVerificationStatus(com.jobportal.entity.VerificationStatus.PENDING);
            user.setCompanyName(signUpRequest.getCompanyName());
            user.setCompanyEmail(signUpRequest.getCompanyEmail());
            user.setCompanyWebsite(signUpRequest.getCompanyWebsite());
            user.setCompanyRegistrationId(signUpRequest.getCompanyRegistrationId());
        }

        userRepository.save(user);

        Map<String, String> response = new HashMap<>();
        response.put("message", "User registered successfully!");
        return ResponseEntity.ok(response);
    }
}
