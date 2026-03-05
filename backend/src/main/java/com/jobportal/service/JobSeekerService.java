package com.jobportal.service;

import com.jobportal.entity.*;
import com.jobportal.repository.ApplicationRepository;
import com.jobportal.repository.JobRepository;
import com.jobportal.repository.ResumeRepository;
import com.jobportal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class JobSeekerService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Value("${upload.path}")
    private String uploadPath;

    public User getProfile(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public Resume getResume(String email) {
        User user = getProfile(email);
        return resumeRepository.findByJobSeeker(user)
                .orElseThrow(() -> new RuntimeException("Resume not found"));
    }

    public List<Job> searchJobs(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return jobRepository.findAllApproved();
        }
        return jobRepository.searchApprovedJobs(keyword);
    }

    public Application applyForJob(Long jobId, String email) {
        User user = getProfile(email);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        if (applicationRepository.existsByJobAndJobSeeker(job, user)) {
            throw new RuntimeException("You have already applied for this job");
        }

        Application application = new Application();
        application.setJob(job);
        application.setJobSeeker(user);
        application.setStatus(ApplicationStatus.APPLIED);
        application.setAppliedAt(LocalDateTime.now());

        return applicationRepository.save(application);
    }

    public List<Application> getMyApplications(String email) {
        User user = getProfile(email);
        return applicationRepository.findByJobSeeker(user);
    }

    public Resume uploadResume(String email, MultipartFile file) throws IOException {
        User user = getProfile(email);

        if (file.isEmpty()) {
            throw new RuntimeException("Please select a file to upload");
        }

        Path uploadDir = Paths.get(uploadPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        String fileName = "resume_" + user.getId() + "_" + System.currentTimeMillis() + "_"
                + file.getOriginalFilename();
        Path filePath = uploadDir.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        Optional<Resume> existingResumeOpt = resumeRepository.findByJobSeeker(user);
        Resume resume;
        if (existingResumeOpt.isPresent()) {
            resume = existingResumeOpt.get();
            // Delete old file if exists
            try {
                Files.deleteIfExists(Paths.get(resume.getFilePath()));
            } catch (Exception e) {
                // Ignore if file doesn't exist
            }
            resume.setFilePath(filePath.toString());
        } else {
            resume = new Resume();
            resume.setJobSeeker(user);
            resume.setFilePath(filePath.toString());
            resume.setUploadedAt(LocalDateTime.now());
        }

        return resumeRepository.save(resume);
    }

    public void deleteResume(String email) {
        User user = getProfile(email);
        Optional<Resume> resumeOpt = resumeRepository.findByJobSeeker(user);
        if (resumeOpt.isPresent()) {
            Resume resume = resumeOpt.get();
            try {
                Files.deleteIfExists(Paths.get(resume.getFilePath()));
            } catch (IOException e) {
                // Log error but continue to delete from DB
            }
            resumeRepository.delete(resume);
        } else {
            throw new RuntimeException("No resume found to delete");
        }
    }

    public User updateProfile(String email, User profileData) {
        User user = getProfile(email);
        user.setName(profileData.getName());
        user.setPhoneNumber(profileData.getPhoneNumber());
        user.setLocation(profileData.getLocation());
        user.setLinkedinUrl(profileData.getLinkedinUrl());
        user.setGithubUrl(profileData.getGithubUrl());
        user.setSkills(profileData.getSkills());
        user.setExperienceLevel(profileData.getExperienceLevel());
        return userRepository.save(user);
    }

    public User uploadProfilePicture(String email, MultipartFile file) throws IOException {
        User user = getProfile(email);

        if (file.isEmpty()) {
            throw new RuntimeException("Please select a file to upload");
        }

        Path uploadDir = Paths.get(uploadPath, "profiles");
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        String fileName = "profile_" + user.getId() + "_" + System.currentTimeMillis() + "_"
                + file.getOriginalFilename();
        Path filePath = uploadDir.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Delete old profile picture if exists
        if (user.getProfilePicturePath() != null) {
            try {
                Files.deleteIfExists(Paths.get(user.getProfilePicturePath()));
            } catch (Exception e) {
                // Ignore
            }
        }

        user.setProfilePicturePath(filePath.toString());
        return userRepository.save(user);
    }

    public java.util.Map<String, Long> getApplicationStats(String email) {
        User user = getProfile(email);
        List<Application> apps = applicationRepository.findByJobSeeker(user);

        java.util.Map<String, Long> stats = new java.util.HashMap<>();
        stats.put("applied", apps.stream().filter(a -> a.getStatus() == ApplicationStatus.APPLIED).count());
        stats.put("shortlisted", apps.stream().filter(a -> a.getStatus() == ApplicationStatus.SHORTLISTED).count());
        stats.put("rejected", apps.stream().filter(a -> a.getStatus() == ApplicationStatus.REJECTED).count());
        stats.put("total", (long) apps.size());

        return stats;
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1. Delete all applications by this seeker
        applicationRepository.deleteByJobSeeker(user);

        // 2. Delete resume
        resumeRepository.deleteByJobSeeker(user);

        // 3. Finally delete the user
        userRepository.delete(user);
    }
}
