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

import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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

    @Value("${upload.path}")
    private String uploadPath;

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

    @PostMapping(value = "/register", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registerUserMultipart(@Valid @ModelAttribute SignupRequest signUpRequest,
            @RequestParam(value = "authLetter", required = false) MultipartFile authLetter) {
        return processRegistration(signUpRequest, authLetter);
    }

    @PostMapping(value = "/register", consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> registerUserJson(@Valid @RequestBody SignupRequest signUpRequest) {
        return processRegistration(signUpRequest, null);
    }

    private ResponseEntity<?> processRegistration(SignupRequest signUpRequest, MultipartFile authLetter) {

        if (signUpRequest.getEmail() == null) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Error: Email is required!");
            return ResponseEntity.badRequest().body(response);
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Error: Email is already in use!");
            return ResponseEntity.badRequest().body(response);
        }

        Role userRole = signUpRequest.getRole() != null ? signUpRequest.getRole() : Role.ROLE_JOB_SEEKER;

        if (userRole != Role.ROLE_RECRUITER && !signUpRequest.getEmail().toLowerCase().endsWith("@gmail.com")) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Error: Only @gmail.com addresses are allowed for job seeker registration!");
            return ResponseEntity.badRequest().body(response);
        }

        if (userRole == Role.ROLE_RECRUITER) {
            String cEmail = signUpRequest.getCompanyEmail();
            String cName = signUpRequest.getCompanyName();
            if (cEmail == null || cEmail.isEmpty() || !cEmail.contains("@") || !cEmail.contains(".")) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Error: Invalid official company email!");
                return ResponseEntity.badRequest().body(response);
            }
            if (cName == null || cName.isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Error: Company name is required!");
                return ResponseEntity.badRequest().body(response);
            }

            String domainPart = cEmail.substring(cEmail.indexOf("@") + 1);
            if (domainPart.contains(".")) {
                domainPart = domainPart.substring(0, domainPart.lastIndexOf("."));
            }

            String cleanCompanyName = cName.replaceAll("\\s", "").toLowerCase();
            if (!domainPart.equalsIgnoreCase(cleanCompanyName)) {
                Map<String, String> response = new HashMap<>();
                response.put("message",
                        "Error: Official email domain must officially match the company name (e.g., if company is Zoho, email must be @zoho.com)!");
                return ResponseEntity.badRequest().body(response);
            }

            if (authLetter == null || authLetter.isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Error: Authorization letter must be provided for recruiter accounts!");
                return ResponseEntity.badRequest().body(response);
            }
        }

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

        user = userRepository.save(user);

        if (userRole == Role.ROLE_RECRUITER && authLetter != null && !authLetter.isEmpty()) {
            try {
                Path dir = Paths.get(uploadPath, "auth_letters");
                if (!Files.exists(dir)) {
                    Files.createDirectories(dir);
                }
                String filename = "auth_" + user.getId() + "_" + System.currentTimeMillis() + "_"
                        + authLetter.getOriginalFilename();
                Path fp = dir.resolve(filename);
                Files.copy(authLetter.getInputStream(), fp, StandardCopyOption.REPLACE_EXISTING);
                user.setAuthLetterPath(fp.toString());
                userRepository.save(user);
            } catch (IOException e) {
                userRepository.delete(user); // rollback if failed
                Map<String, String> response = new HashMap<>();
                response.put("message", "Error: Failed to save authorization letter. " + e.getMessage());
                return ResponseEntity.internalServerError().body(response);
            }
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "User registered successfully!");
        return ResponseEntity.ok(response);
    }
}
