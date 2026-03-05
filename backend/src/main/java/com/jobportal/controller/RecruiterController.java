package com.jobportal.controller;

import com.jobportal.entity.ApplicationStatus;
import com.jobportal.entity.Job;
import com.jobportal.entity.Resume;
import com.jobportal.entity.User;
import com.jobportal.service.RecruiterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recruiter")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('RECRUITER')")
public class RecruiterController {

    @Autowired
    private RecruiterService recruiterService;

    @GetMapping("/profile")
    public ResponseEntity<User> getProfile(Authentication authentication) {
        return ResponseEntity.ok(recruiterService.getProfile(authentication.getName()));
    }

    @PostMapping("/jobs")
    public ResponseEntity<?> postJob(@RequestBody Job jobRequest, Authentication authentication) {
        try {
            return ResponseEntity.ok(recruiterService.postJob(jobRequest, authentication.getName()));
        } catch (RuntimeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/jobs/{id}")
    public ResponseEntity<?> editJob(@PathVariable Long id, @RequestBody Job jobRequest,
            Authentication authentication) {
        try {
            return ResponseEntity.ok(recruiterService.editJob(id, jobRequest, authentication.getName()));
        } catch (RuntimeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/jobs/{id}")
    public ResponseEntity<?> deleteJob(@PathVariable Long id, Authentication authentication) {
        try {
            recruiterService.deleteJob(id, authentication.getName());
            Map<String, String> response = new HashMap<>();
            response.put("message", "Job deleted successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<Job>> getMyJobs(Authentication authentication) {
        return ResponseEntity.ok(recruiterService.getMyJobs(authentication.getName()));
    }

    @GetMapping("/jobs/{jobId}/applications")
    public ResponseEntity<?> getApplicantsForJob(@PathVariable Long jobId, Authentication authentication) {
        try {
            return ResponseEntity.ok(recruiterService.getApplicantsForJob(jobId, authentication.getName()));
        } catch (RuntimeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/applications/{applicationId}/status")
    public ResponseEntity<?> updateApplicationStatus(@PathVariable Long applicationId,
            @RequestParam ApplicationStatus status,
            Authentication authentication) {
        try {
            return ResponseEntity
                    .ok(recruiterService.updateApplicationStatus(applicationId, status, authentication.getName()));
        } catch (RuntimeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/resume/{seekerId}")
    public ResponseEntity<?> getResume(@PathVariable Long seekerId) {
        try {
            return ResponseEntity.ok(recruiterService.getResumeBySeekerId(seekerId));
        } catch (RuntimeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/resume/download/{seekerId}")
    public ResponseEntity<?> viewResume(@PathVariable Long seekerId) {
        try {
            Resume resume = recruiterService.getResumeBySeekerId(seekerId);
            Path path = Paths.get(resume.getFilePath());
            Resource resource = new UrlResource(path.toUri());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/seeker-profile-pic/{seekerId}")
    public ResponseEntity<?> getSeekerProfilePicture(@PathVariable Long seekerId) {
        try {
            User seeker = recruiterService.getSeekerById(seekerId);
            if (seeker.getProfilePicturePath() == null) {
                return ResponseEntity.notFound().build();
            }
            Path path = Paths.get(seeker.getProfilePicturePath());
            Resource resource = new UrlResource(path.toUri());

            String contentType = "image/jpeg";
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

    @DeleteMapping("/account")
    public ResponseEntity<?> deleteAccount(java.security.Principal principal) {
        try {
            recruiterService.deleteAccount(principal.getName());
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
