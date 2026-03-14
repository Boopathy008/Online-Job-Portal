package com.jobportal.controller;

import com.jobportal.entity.Application;
import com.jobportal.entity.Job;
import com.jobportal.entity.Resume;
import com.jobportal.entity.Certificate;
import com.jobportal.entity.User;
import com.jobportal.service.JobSeekerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seeker")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('JOB_SEEKER')")
public class JobSeekerController {

    @Autowired
    private JobSeekerService jobSeekerService;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        User user = jobSeekerService.getProfile(authentication.getName());
        return ResponseEntity.ok(user);
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<Job>> searchJobs(@RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(jobSeekerService.searchJobs(keyword));
    }

    @PostMapping("/apply/{jobId}")
    public ResponseEntity<?> applyForJob(@PathVariable Long jobId, Authentication authentication) {
        try {
            Application application = jobSeekerService.applyForJob(jobId, authentication.getName());
            return ResponseEntity.ok(application);
        } catch (RuntimeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/applications")
    public ResponseEntity<List<Application>> getMyApplications(Authentication authentication) {
        return ResponseEntity.ok(jobSeekerService.getMyApplications(authentication.getName()));
    }

    @GetMapping("/resume")
    public ResponseEntity<?> getResume(Authentication authentication) {
        try {
            return ResponseEntity.ok(jobSeekerService.getResume(authentication.getName()));
        } catch (RuntimeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/resume/upload")
    public ResponseEntity<?> uploadResume(@RequestParam("file") MultipartFile file, Authentication authentication) {
        try {
            Resume resume = jobSeekerService.uploadResume(authentication.getName(), file);
            return ResponseEntity.ok(resume);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Could not upload the file: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/profile/picture")
    public ResponseEntity<?> getProfilePicture(Authentication authentication) {
        try {
            User user = jobSeekerService.getProfile(authentication.getName());
            if (user.getProfilePicturePath() == null) {
                return ResponseEntity.notFound().build();
            }
            Path path = Paths.get(user.getProfilePicturePath());
            Resource resource = new UrlResource(path.toUri());

            String contentType = "image/jpeg"; // Default
            if (path.toString().endsWith(".png"))
                contentType = "image/png";
            else if (path.toString().endsWith(".gif"))
                contentType = "image/gif";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/resume")
    public ResponseEntity<?> deleteResume(Authentication authentication) {
        try {
            jobSeekerService.deleteResume(authentication.getName());
            Map<String, String> response = new HashMap<>();
            response.put("message", "Resume deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody User profileData, Authentication authentication) {
        try {
            User updatedUser = jobSeekerService.updateProfile(authentication.getName(), profileData);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/profile/picture")
    public ResponseEntity<?> uploadProfilePicture(@RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            User user = jobSeekerService.uploadProfilePicture(authentication.getName(), file);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Could not upload profile picture: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats(Authentication authentication) {
        return ResponseEntity.ok(jobSeekerService.getApplicationStats(authentication.getName()));
    }

    @GetMapping("/resume/download")
    public ResponseEntity<Resource> downloadResume(Authentication authentication) {
        try {
            Resume resume = jobSeekerService.getResume(authentication.getName());
            Path path = Paths.get(resume.getFilePath());
            Resource resource = new UrlResource(path.toUri());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/certificates/upload")
    public ResponseEntity<?> uploadCertificate(@RequestParam("file") MultipartFile file, Authentication authentication) {
        try {
            Certificate cert = jobSeekerService.uploadCertificate(authentication.getName(), file);
            return ResponseEntity.ok(cert);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Could not upload certificate: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/certificates")
    public ResponseEntity<List<Certificate>> getCertificates(Authentication authentication) {
        return ResponseEntity.ok(jobSeekerService.getCertificates(authentication.getName()));
    }

    @GetMapping("/certificates/download/{id}")
    public ResponseEntity<Resource> downloadCertificate(@PathVariable Long id, Authentication authentication) {
        try {
            // Service handles authorization implicitly by checking if it belongs to the user
            Certificate cert = jobSeekerService.getCertificateById(id);
            // Extra security check for the controller
            if (!cert.getJobSeeker().getEmail().equals(authentication.getName())) {
               return ResponseEntity.status(403).build();
            }

            Path path = Paths.get(cert.getFilePath());
            Resource resource = new UrlResource(path.toUri());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/certificates/{id}")
    public ResponseEntity<?> deleteCertificate(@PathVariable Long id, Authentication authentication) {
        try {
            jobSeekerService.deleteCertificate(authentication.getName(), id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Certificate deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }



    @DeleteMapping("/account")
    public ResponseEntity<?> deleteAccount(java.security.Principal principal) {
        try {
            jobSeekerService.deleteAccount(principal.getName());
            Map<String, String> response = new HashMap<>();
            response.put("message", "Account deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
