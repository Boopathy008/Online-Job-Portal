package com.jobportal.controller;

import com.jobportal.entity.Job;
import com.jobportal.entity.Role;
import com.jobportal.entity.User;
import com.jobportal.entity.VerificationStatus;
import com.jobportal.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<Job>> getAllJobs() {
        return ResponseEntity.ok(adminService.getAllJobs());
    }

    @DeleteMapping("/jobs/{id}")
    public ResponseEntity<?> deleteJob(@PathVariable Long id) {
        try {
            adminService.deleteJob(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Job deleted successfully by admin");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> changeUserRole(@PathVariable Long id, @RequestParam Role role) {
        try {
            User updatedUser = adminService.changeUserRole(id, role);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/recruiters/pending")
    public ResponseEntity<List<User>> getPendingRecruiters() {
        return ResponseEntity.ok(adminService.getPendingRecruiters());
    }

    @PutMapping("/recruiters/{id}/status")
    public ResponseEntity<?> updateRecruiterStatus(@PathVariable Long id,
            @RequestParam VerificationStatus status,
            @RequestParam(required = false) String message) {
        try {
            return ResponseEntity.ok(adminService.updateRecruiterStatus(id, status, message));
        } catch (RuntimeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/recruiters/{id}/auth-letter")
    public ResponseEntity<Resource> downloadAuthLetter(@PathVariable Long id) {
        try {
            String pathStr = adminService.getAuthLetterPath(id);
            Path filePath = Paths.get(pathStr);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                String contentType = "application/octet-stream";
                if (pathStr.toLowerCase().endsWith(".pdf"))
                    contentType = "application/pdf";
                else if (pathStr.toLowerCase().endsWith(".png"))
                    contentType = "image/png";
                else if (pathStr.toLowerCase().endsWith(".jpg") || pathStr.toLowerCase().endsWith(".jpeg"))
                    contentType = "image/jpeg";

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                throw new RuntimeException("Could not read file");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            adminService.deleteUser(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "User and all associated data deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
